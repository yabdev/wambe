package com.wambe.api.retention;

import com.wambe.api.common.rls.RlsContext;
import com.wambe.api.generated.model.InternalJobResult;
import com.wambe.api.integration.scanner.ScannerDispatchPort;
import com.wambe.api.integration.storage.ObjectStoragePort;
import com.wambe.api.media.persistence.MediaEntity;
import com.wambe.api.media.persistence.MediaRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InternalJobService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactions;
    private final RlsContext rls;
    private final MediaRepository media;
    private final ScannerDispatchPort scanner;
    private final ObjectStoragePort storage;

    public InternalJobService(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactions,
            RlsContext rls,
            MediaRepository media,
            ScannerDispatchPort scanner,
            ObjectStoragePort storage) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactions = transactions;
        this.rls = rls;
        this.media = media;
        this.scanner = scanner;
        this.storage = storage;
    }

    public InternalJobResult dispatchDueScans() {
        List<LeasedJob> jobs = transactions.execute(status -> jdbcTemplate.query(
                "select * from lease_due_scan_jobs(20)",
                (resultSet, rowNum) -> new LeasedJob(
                        resultSet.getObject("job_id", UUID.class),
                        resultSet.getObject("media_id", UUID.class),
                        resultSet.getObject("owner_id", UUID.class))));
        int affected = 0;
        if (jobs != null) {
            for (LeasedJob job : jobs) {
                try {
                    MediaEntity item = transactions.execute(status -> {
                        rls.apply(job.ownerId());
                        return media.findByIdAndOwnerId(job.mediaId(), job.ownerId()).orElseThrow();
                    });
                    scanner.dispatch(job.jobId(), item);
                    affected++;
                } catch (RuntimeException exception) {
                    markFailure(job, exception.getClass().getSimpleName());
                }
            }
        }
        return new InternalJobResult(
                InternalJobResult.JobEnum.SCAN_DISPATCH,
                now(),
                jobs == null ? 0 : jobs.size(),
                affected);
    }

    public InternalJobResult runRetention() {
        OffsetDateTime runAt = now();
        List<StoragePaths> paths = transactions.execute(status -> jdbcTemplate.query(
                "select * from retention_storage_paths(?)",
                (resultSet, rowNum) -> new StoragePaths(
                        resultSet.getString("quarantine_path"),
                        resultSet.getString("active_path"),
                        resultSet.getString("preview_path")),
                runAt));
        int deletedObjects = 0;
        if (paths != null) {
            for (StoragePaths path : paths) {
                deletedObjects += delete(path.quarantinePath());
                deletedObjects += delete(path.activePath());
                deletedObjects += delete(path.previewPath());
            }
        }
        Counts counts = transactions.execute(status -> jdbcTemplate.queryForObject(
                "select * from run_wambe_retention(?)",
                (resultSet, rowNum) -> new Counts(
                        resultSet.getLong("examined"),
                        resultSet.getLong("affected")),
                runAt));
        return new InternalJobResult(
                InternalJobResult.JobEnum.RETENTION,
                runAt,
                counts == null ? 0 : Math.toIntExact(counts.examined()),
                counts == null ? deletedObjects : Math.toIntExact(counts.affected()) + deletedObjects);
    }

    private void markFailure(LeasedJob job, String errorCode) {
        transactions.executeWithoutResult(status -> {
            rls.apply(job.ownerId());
            jdbcTemplate.update("""
                    update scan_jobs
                       set status = 'failed',
                           last_error_code = ?,
                           lease_expires_at = null,
                           next_attempt_at = now() + case
                               when attempts <= 1 then interval '1 minute'
                               when attempts = 2 then interval '5 minutes'
                               else interval '20 minutes'
                           end,
                           updated_at = now()
                     where id = ? and owner_id = ?
                    """,
                    errorCode,
                    job.jobId(),
                    job.ownerId());
        });
    }

    private int delete(String path) {
        if (path == null) {
            return 0;
        }
        storage.delete(path);
        return 1;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private record LeasedJob(UUID jobId, UUID mediaId, UUID ownerId) {
    }

    private record StoragePaths(String quarantinePath, String activePath, String previewPath) {
    }

    private record Counts(long examined, long affected) {
    }
}

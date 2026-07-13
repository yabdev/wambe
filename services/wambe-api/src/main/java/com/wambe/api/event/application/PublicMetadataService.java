package com.wambe.api.event.application;

import com.wambe.api.common.error.ApiException;
import com.wambe.api.generated.model.PublicEventMetadata;
import com.wambe.api.integration.storage.ObjectStoragePort;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicMetadataService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectStoragePort storage;
    private final String publicBaseUrl;

    public PublicMetadataService(
            JdbcTemplate jdbcTemplate,
            ObjectStoragePort storage,
            @Value("${wambe.public-base-url}") String publicBaseUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.storage = storage;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional(readOnly = true)
    public PublicEventMetadata get(String slug) {
        List<Row> rows = jdbcTemplate.query(
                "select * from safe_event_metadata(?)",
                (resultSet, rowNum) -> map(resultSet),
                slug);
        if (rows.isEmpty()) {
            throw ApiException.notFound();
        }
        Row row = rows.getFirst();
        if (row.deleted()) {
            throw new ApiException(HttpStatus.GONE, "EVENT_DELETED", "This event is no longer available");
        }
        var response = new PublicEventMetadata(
                row.slug(),
                row.title(),
                row.startsAt(),
                PublicEventMetadata.VisibilityEnum.fromValue(row.visibility()),
                row.indexable(),
                URI.create(publicBaseUrl + "/e/" + row.slug()));
        response.venueName(row.venueName());
        response.venueAddress(row.venueAddress());
        if (row.previewPath() != null) {
            response.invitationPreviewUrl(storage.signedGetActive(
                    row.previewPath(), Duration.ofMinutes(10)));
        }
        return response;
    }

    private Row map(ResultSet resultSet) throws SQLException {
        return new Row(
                resultSet.getString("slug"),
                resultSet.getString("title"),
                resultSet.getObject("starts_at", OffsetDateTime.class),
                resultSet.getString("visibility"),
                resultSet.getBoolean("indexable"),
                resultSet.getString("venue_name"),
                resultSet.getString("venue_address"),
                resultSet.getString("preview_path"),
                resultSet.getBoolean("deleted"));
    }

    private record Row(
            String slug,
            String title,
            OffsetDateTime startsAt,
            String visibility,
            boolean indexable,
            String venueName,
            String venueAddress,
            String previewPath,
            boolean deleted) {
    }
}

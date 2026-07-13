import { PublishSuccess } from "@/components/events/PublishSuccess";

export default async function PublishedPage({
  params,
}: {
  params: Promise<{ eventId: string }>;
}) {
  const { eventId } = await params;
  return <PublishSuccess eventId={eventId} />;
}

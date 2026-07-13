import { EventEditor } from "@/components/events/EventEditor";

export default async function EditEventPage({
  params,
}: {
  params: Promise<{ eventId: string }>;
}) {
  const { eventId } = await params;
  return <EventEditor eventId={eventId} />;
}

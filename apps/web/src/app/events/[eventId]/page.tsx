import { EventManager } from "@/components/events/EventManager";

export default async function ManageEventPage({
  params,
}: {
  params: Promise<{ eventId: string }>;
}) {
  const { eventId } = await params;
  return <EventManager eventId={eventId} />;
}

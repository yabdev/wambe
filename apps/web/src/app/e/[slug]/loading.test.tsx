import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import LoadingPublicEvent from "./loading";

describe("public event loading shell", () => {
  it("keeps a named, busy invitation frame in the document", () => {
    render(<LoadingPublicEvent />);

    const shell = screen.getByRole("status", {
      name: "Opening your invitation",
    });
    expect(shell).toHaveAttribute("aria-busy", "true");
    expect(screen.getByRole("main")).toContainElement(shell);
    expect(screen.getByText("Celebrated with")).toBeVisible();
  });
});

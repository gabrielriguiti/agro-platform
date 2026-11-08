import "@testing-library/jest-dom/vitest";
import { afterEach, vi } from "vitest";

// Cada teste começa sem `fetch` simulado nem timers falsos do teste anterior.
afterEach(() => {
  vi.unstubAllGlobals();
  vi.useRealTimers();
});

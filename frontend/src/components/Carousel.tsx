"use client";

/* eslint-disable @next/next/no-img-element -- self-hosted /public photos, served
   as-is; next/image would pull in the optimizer (and sharp) for no gain here. */

import { useCallback, useEffect, useState } from "react";

type Slide = { src: string; alt: string; caption: string };

const SLIDES: Slide[] = [
  {
    src: "/carousel/reception.jpg",
    alt: "A clinic reception desk",
    caption: "One front desk, one address — your clinic at its own subdomain.",
  },
  {
    src: "/carousel/care.jpg",
    alt: "A clinician talking with a patient",
    caption: "Keep your people and your care teams in one place.",
  },
  {
    src: "/carousel/ward.jpg",
    alt: "A hospital ward with beds",
    caption: "Units and wards, tracked across every location.",
  },
  {
    src: "/carousel/theatre.jpg",
    alt: "An operating theatre",
    caption: "From reception to theatre, one system per clinic.",
  },
];

const INTERVAL = 5000;

export function Carousel() {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const count = SLIDES.length;

  const go = useCallback((step: number) => setIndex((i) => (i + step + count) % count), [count]);

  useEffect(() => {
    if (paused) return;
    const id = setInterval(() => setIndex((i) => (i + 1) % count), INTERVAL);
    return () => clearInterval(id);
  }, [paused, count]);

  return (
    <div
      className="group relative aspect-[16/10] overflow-hidden rounded-2xl border border-line bg-surface-muted shadow-sm"
      role="region"
      aria-roledescription="carousel"
      aria-label="A clinic in Kliniku"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
      onKeyDown={(e) => {
        if (e.key === "ArrowLeft") go(-1);
        if (e.key === "ArrowRight") go(1);
      }}
      tabIndex={0}
    >
      {SLIDES.map((s, i) => (
        <figure
          key={s.src}
          className={`absolute inset-0 m-0 transition-opacity duration-700 ease-out ${
            i === index ? "opacity-100" : "pointer-events-none opacity-0"
          }`}
          aria-hidden={i !== index}
        >
          <img
            src={s.src}
            alt={s.alt}
            loading={i === 0 ? "eager" : "lazy"}
            className="size-full object-cover"
          />
          <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/70 via-black/25 to-transparent p-5 pt-16">
            <figcaption className="text-sm font-medium text-white sm:text-base">{s.caption}</figcaption>
          </div>
        </figure>
      ))}

      {/* Arrows — appear on hover/focus of the region. */}
      <button
        type="button"
        onClick={() => go(-1)}
        aria-label="Previous slide"
        className="absolute top-1/2 left-3 grid size-9 -translate-y-1/2 place-items-center rounded-full bg-black/40 text-white opacity-0 backdrop-blur transition group-hover:opacity-100 focus-visible:opacity-100 hover:bg-black/60"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="size-5">
          <path d="m15 6-6 6 6 6" />
        </svg>
      </button>
      <button
        type="button"
        onClick={() => go(1)}
        aria-label="Next slide"
        className="absolute top-1/2 right-3 grid size-9 -translate-y-1/2 place-items-center rounded-full bg-black/40 text-white opacity-0 backdrop-blur transition group-hover:opacity-100 focus-visible:opacity-100 hover:bg-black/60"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="size-5">
          <path d="m9 6 6 6-6 6" />
        </svg>
      </button>

      {/* Dots. */}
      <div className="absolute inset-x-0 bottom-3 flex items-center justify-center gap-2">
        {SLIDES.map((s, i) => (
          <button
            key={s.src}
            type="button"
            onClick={() => setIndex(i)}
            aria-label={`Go to slide ${i + 1}`}
            aria-current={i === index}
            className={`h-1.5 rounded-full transition-all ${
              i === index ? "w-6 bg-white" : "w-1.5 bg-white/50 hover:bg-white/80"
            }`}
          />
        ))}
      </div>
    </div>
  );
}

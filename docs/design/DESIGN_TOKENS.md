# DESS Design Tokens

These values are the canonical visual tokens for DESS v1.1. Reuse them; do not create page-specific alternatives without design-system review.

Tokens live in `src/main/webapp/assets/css/tokens.css`. Page styles compose them instead of repeating raw values.

## Colors

| Token | Value | Intended use |
|---|---|---|
| `--dess-primary` | `#172C43` | Sidebar, primary headings, pressed primary action |
| `--dess-accent` | `#347C78` | Primary actions, selected states, active indicators |
| `--dess-accent-strong` | `#2A6663` | Hover and active state for primary actions and links |
| `--dess-accent-soft` | `#E8F0EF` | Selected row, checked choice, subtle accent background |
| `--dess-background` | `#F5F6F4` | Application canvas |
| `--dess-surface` | `#FFFFFF` | Panels, forms, tables, dialogs |
| `--dess-overlay` | `rgb(23 44 67 / 45%)` | Dialog backdrop and navigation scrim |
| `--dess-text` | `#263746` | Main text |
| `--dess-text-muted` | `#5F6F82` | Secondary text and metadata |
| `--dess-on-dark` | `#FFFFFF` | Text and icons on the primary sidebar |
| `--dess-on-dark-muted` | `#B6C0CC` | Group headings and footer text on the sidebar |
| `--dess-border` | `#E2E7E9` | Dividers, panel borders, input outlines |
| `--dess-border-strong` | `#CBD4D9` | Hovered control outline |
| `--dess-border-on-dark` | `#33475C` | Dividers on the sidebar |
| `--dess-success` | `#397A59` | Successful state |
| `--dess-success-soft` | `#EDF5F0` | Success alert background |
| `--dess-warning` | `#D69B45` | Warning state marker |
| `--dess-warning-strong` | `#8A5A12` | Warning text and badges |
| `--dess-warning-soft` | `#FBF4E6` | Warning alert background |
| `--dess-danger` | `#B94C4C` | Error and destructive state |
| `--dess-danger-strong` | `#9E3E3E` | Error text, destructive hover |
| `--dess-danger-soft` | `#F9EDED` | Error alert background |
| `--dess-info` | `#477BA5` | Informational state |
| `--dess-info-soft` | `#EDF3F8` | Information alert background |

### Contrast baseline

All text tokens meet WCAG 2.2 AA (4.5:1) on the surfaces where they are used.

| Pair | Ratio |
|---|---:|
| `--dess-text` on `--dess-surface` | 12.2:1 |
| `--dess-text-muted` on `--dess-surface` | 5.1:1 |
| `--dess-text-muted` on `--dess-background` | 4.7:1 |
| `--dess-surface` on `--dess-accent` | 4.9:1 |
| `--dess-surface` on `--dess-primary` | 14.2:1 |
| `--dess-on-dark-muted` on `--dess-primary` | 7.7:1 |
| `--dess-accent-strong` on `--dess-surface` | 6.6:1 |
| `--dess-success` on `--dess-surface` | 5.1:1 |
| `--dess-danger-strong` on `--dess-surface` | 6.2:1 |

`--dess-warning` is a marker colour only. Warning text uses `--dess-warning-strong`.

## Typography

```css
:root {
    --dess-font-family: Inter, -apple-system, BlinkMacSystemFont,
        "Segoe UI", sans-serif;

    --dess-font-size-brand: 1.125rem; /* 18px, sidebar brand */
    --dess-font-size-page-title: 1.625rem; /* 26px */
    --dess-font-size-section-title: 1.1875rem; /* 19px */
    --dess-font-size-component-title: 0.9375rem; /* 15px */
    --dess-font-size-body: 0.875rem; /* 14px */
    --dess-font-size-table: 0.8125rem; /* 13px */
    --dess-font-size-label: 0.75rem; /* 12px */
    --dess-font-size-meta: 0.6875rem; /* 11px */
}
```

Use `--dess-font-size-label` for interface labels, hints, badges and navigation group headings. Reserve `--dess-font-size-meta` for dense metadata such as timestamps inside a table or a footer line, never for an action or a required message.

## Layout, controls and spacing

Use a 4px base spacing unit. `--dess-space-0` is a 2px hairline for stacked labels and list gaps.

| Token | Value | Use |
|---|---:|---|
| `--dess-space-0` | 2px | Hairline gap inside a label or list |
| `--dess-space-1` | 4px | Tight inline gap |
| `--dess-space-2` | 8px | Compact control gap |
| `--dess-space-3` | 12px | Related element gap |
| `--dess-space-4` | 16px | Standard component spacing |
| `--dess-space-5` | 20px | Section internal spacing |
| `--dess-space-6` | 24px | Standard page gutter |
| `--dess-space-8` | 32px | Large section spacing |
| `--dess-content-max` | 1440px | Maximum content width |
| `--dess-sidebar-width` | 250px | Desktop sidebar |
| `--dess-sidebar-drawer-width` | `min(86vw, 300px)` | Sidebar drawer |
| `--dess-header-height` | 64px | Top navigation |
| `--dess-brand-mark-size` | 38px | Sidebar brand square |
| `--dess-nav-indicator-width` | 3px | Active navigation rail |
| `--dess-control-height` | 40px | Button and input height |
| `--dess-control-height-sm` | 32px | Compact button height |
| `--dess-touch-target` | 44px | Minimum target on coarse pointers |
| `--dess-dialog-width` | 720px | Default dialog width |

At `pointer: coarse`, buttons, icon buttons, inputs, selects and text areas grow to `--dess-touch-target`. A page may override `--dess-dialog-width` locally when a shorter dialog reads better.

## Shape and depth

```css
:root {
    --dess-radius-sm: 4px;
    --dess-radius-md: 8px;
    --dess-radius-lg: 12px;
    --dess-border-width: 1px;
    --dess-shadow-panel: 0 1px 3px rgb(23 44 67 / 7%);
    --dess-shadow-dialog: 0 18px 48px rgb(23 44 67 / 20%);
}
```

Use shadows sparingly. Borders provide most of the separation. Only dialogs use `--dess-shadow-dialog`.

## Focus and motion

```css
:root {
    --dess-focus-ring: 0 0 0 2px var(--dess-surface),
        0 0 0 4px var(--dess-accent);
    --dess-focus-ring-on-dark: 0 0 0 2px var(--dess-primary),
        0 0 0 4px var(--dess-on-dark);
    --dess-transition-fast: 120ms ease;
    --dess-transition-drawer: 180ms ease;
}
```

The focus ring is a solid two-layer ring: a 2px surface gap, then a 2px accent edge. The accent reaches at least 4.5:1 against the canvas and the surface, so the ring stays visible. On the navy sidebar use `--dess-focus-ring-on-dark`; the accent does not separate from `--dess-primary`.

Use `--dess-transition-fast` for hover, active and focus colour changes. Use `--dess-transition-drawer` only for the sidebar drawer. All motion is disabled by `prefers-reduced-motion`.

## CSS starter file

The canonical file is `src/main/webapp/assets/css/tokens.css`. The following is its token block.

```css
:root {
    color-scheme: light;

    --dess-primary: #172C43;
    --dess-accent: #347C78;
    --dess-accent-strong: #2A6663;
    --dess-accent-soft: #E8F0EF;
    --dess-background: #F5F6F4;
    --dess-surface: #FFFFFF;
    --dess-overlay: rgb(23 44 67 / 45%);

    --dess-text: #263746;
    --dess-text-muted: #5F6F82;
    --dess-on-dark: #FFFFFF;
    --dess-on-dark-muted: #B6C0CC;
    --dess-on-dark-hover: rgb(255 255 255 / 8%);
    --dess-on-dark-active: rgb(255 255 255 / 12%);

    --dess-border: #E2E7E9;
    --dess-border-strong: #CBD4D9;
    --dess-border-on-dark: #33475C;

    --dess-success: #397A59;
    --dess-success-soft: #EDF5F0;
    --dess-warning: #D69B45;
    --dess-warning-strong: #8A5A12;
    --dess-warning-soft: #FBF4E6;
    --dess-danger: #B94C4C;
    --dess-danger-strong: #9E3E3E;
    --dess-danger-soft: #F9EDED;
    --dess-info: #477BA5;
    --dess-info-soft: #EDF3F8;

    --dess-font-family: Inter, -apple-system, BlinkMacSystemFont,
        "Segoe UI", sans-serif;

    --dess-font-size-brand: 1.125rem;
    --dess-font-size-page-title: 1.625rem;
    --dess-font-size-section-title: 1.1875rem;
    --dess-font-size-component-title: 0.9375rem;
    --dess-font-size-body: 0.875rem;
    --dess-font-size-table: 0.8125rem;
    --dess-font-size-label: 0.75rem;
    --dess-font-size-meta: 0.6875rem;

    --dess-space-0: 2px;
    --dess-space-1: 4px;
    --dess-space-2: 8px;
    --dess-space-3: 12px;
    --dess-space-4: 16px;
    --dess-space-5: 20px;
    --dess-space-6: 24px;
    --dess-space-8: 32px;

    --dess-content-max: 1440px;
    --dess-sidebar-width: 250px;
    --dess-sidebar-drawer-width: min(86vw, 300px);
    --dess-header-height: 64px;
    --dess-brand-mark-size: 38px;
    --dess-nav-indicator-width: 3px;
    --dess-control-height: 40px;
    --dess-control-height-sm: 32px;
    --dess-touch-target: 44px;
    --dess-dialog-width: 720px;

    --dess-radius-sm: 4px;
    --dess-radius-md: 8px;
    --dess-radius-lg: 12px;
    --dess-border-width: 1px;
    --dess-shadow-panel: 0 1px 3px rgb(23 44 67 / 7%);
    --dess-shadow-dialog: 0 18px 48px rgb(23 44 67 / 20%);

    --dess-focus-ring: 0 0 0 2px var(--dess-surface),
        0 0 0 4px var(--dess-accent);
    --dess-focus-ring-on-dark: 0 0 0 2px var(--dess-primary),
        0 0 0 4px var(--dess-on-dark);

    --dess-transition-fast: 120ms ease;
    --dess-transition-drawer: 180ms ease;
}
```

## Usage rules

- Use CSS custom properties rather than repeating raw values.
- Bootstrap may provide layout primitives, but override its colors and component appearance through DESS tokens.
- Do not use the accent color for large background areas.
- Keep disabled, hover, active, focus, selected, and error states visually distinct.
- Do not rely on colour alone: pair every status colour with text or a shape.
- Check text contrast against the actual background, including soft alert tints.
- Change a token in `tokens.css` and this document together.

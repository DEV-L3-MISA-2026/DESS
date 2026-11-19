# DESS Design Tokens

These values are the canonical visual tokens for DESS v1.0. Reuse them; do not create page-specific alternatives without design-system review.

## Colors

| Token | Value | Intended use |
|---|---|---|
| `--dess-primary` | `#172C43` | Sidebar, primary headings, strong navigation |
| `--dess-accent` | `#347C78` | Primary actions, selected states, active indicators |
| `--dess-background` | `#F5F6F4` | Application canvas |
| `--dess-surface` | `#FFFFFF` | Panels, forms, tables, dialogs |
| `--dess-text` | `#263746` | Main text |
| `--dess-text-muted` | `#718096` | Secondary text and metadata |
| `--dess-border` | `#E2E7E9` | Dividers, borders, input outlines |
| `--dess-success` | `#397A59` | Successful state |
| `--dess-warning` | `#D69B45` | Warning state |
| `--dess-danger` | `#B94C4C` | Error/destructive state |
| `--dess-info` | `#477BA5` | Informational state |

## Typography

```css
:root {
    --dess-font-family: Inter, -apple-system, BlinkMacSystemFont,
        "Segoe UI", sans-serif;

    --dess-font-size-page-title: 1.625rem; /* 26px */
    --dess-font-size-section-title: 1.1875rem; /* 19px */
    --dess-font-size-component-title: 0.9375rem; /* 15px */
    --dess-font-size-body: 0.875rem; /* 14px */
    --dess-font-size-table: 0.8125rem; /* 13px */
    --dess-font-size-label: 0.75rem; /* 12px */
    --dess-font-size-meta: 0.6875rem; /* 11px */
}
```

## Layout and spacing

Use a 4px base spacing unit.

| Token | Value | Use |
|---|---:|---|
| `--dess-space-1` | 4px | Tight inline gap |
| `--dess-space-2` | 8px | Compact control gap |
| `--dess-space-3` | 12px | Related element gap |
| `--dess-space-4` | 16px | Standard component spacing |
| `--dess-space-5` | 20px | Section internal spacing |
| `--dess-space-6` | 24px | Standard page gutter |
| `--dess-space-8` | 32px | Large section spacing |
| `--dess-sidebar-width` | 250px | Desktop sidebar |
| `--dess-header-height` | 64px | Top navigation |

## Shape and borders

```css
:root {
    --dess-radius-sm: 4px;
    --dess-radius-md: 8px;
    --dess-radius-lg: 12px;
    --dess-border-width: 1px;
    --dess-shadow-panel: 0 1px 3px rgb(23 44 67 / 7%);
    --dess-focus-ring: 0 0 0 3px rgb(52 124 120 / 22%);
}
```

Use shadows sparingly. Borders should provide most of the visual separation.

## CSS starter file

The following can be copied into `src/main/webapp/assets/css/tokens.css`.

```css
:root {
    color-scheme: light;

    --dess-primary: #172C43;
    --dess-accent: #347C78;
    --dess-background: #F5F6F4;
    --dess-surface: #FFFFFF;
    --dess-text: #263746;
    --dess-text-muted: #718096;
    --dess-border: #E2E7E9;
    --dess-success: #397A59;
    --dess-warning: #D69B45;
    --dess-danger: #B94C4C;
    --dess-info: #477BA5;

    --dess-font-family: Inter, -apple-system, BlinkMacSystemFont,
        "Segoe UI", sans-serif;

    --dess-font-size-page-title: 1.625rem;
    --dess-font-size-section-title: 1.1875rem;
    --dess-font-size-component-title: 0.9375rem;
    --dess-font-size-body: 0.875rem;
    --dess-font-size-table: 0.8125rem;
    --dess-font-size-label: 0.75rem;
    --dess-font-size-meta: 0.6875rem;

    --dess-space-1: 4px;
    --dess-space-2: 8px;
    --dess-space-3: 12px;
    --dess-space-4: 16px;
    --dess-space-5: 20px;
    --dess-space-6: 24px;
    --dess-space-8: 32px;

    --dess-sidebar-width: 250px;
    --dess-header-height: 64px;

    --dess-radius-sm: 4px;
    --dess-radius-md: 8px;
    --dess-radius-lg: 12px;
    --dess-border-width: 1px;
    --dess-shadow-panel: 0 1px 3px rgb(23 44 67 / 7%);
    --dess-focus-ring: 0 0 0 3px rgb(52 124 120 / 22%);
}

html {
    font-family: var(--dess-font-family);
    color: var(--dess-text);
    background: var(--dess-background);
}

body {
    margin: 0;
    font-size: var(--dess-font-size-body);
    line-height: 1.5;
    color: var(--dess-text);
    background: var(--dess-background);
}

:focus-visible {
    outline: none;
    box-shadow: var(--dess-focus-ring);
}
```

## Usage rules
- Use CSS custom properties rather than repeating raw values.
- Bootstrap may provide layout primitives, but override its colors and component appearance through DESS tokens.
- Do not use the accent color for large background areas.
- Ensure text and interactive controls meet WCAG contrast expectations.
- Keep disabled, hover, focus, selected, and error states visually distinct.

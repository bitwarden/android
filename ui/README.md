# Data module

An Android library containing common UI components, types, and utilities.

## Contents

- [Compatibility](#compatibility)
- [Theme](#theme)

## Compatibility

- **Minimum SDK**: 28
- **Target SDK**: 35

## Theme

### Icons & Illustrations

#### Naming Convention

All drawables should be named with the appropriate prefix to identify what they are and how they are intended to be used.

| Prefix  | Description   |
|---------|---------------|
| `gif_`  | gifs          |
| `ic_`   | Icons         |
| `img_`  | Raster Images |
| `ill_`  | Illustrations |
| `logo_` | Brand Imagery |

#### Multi-tonal Illustrations

The app supports light mode, dark mode and dynamic colors. Most icons in the app will display correctly using tinting but multi-tonal icons and illustrations require extra processing in order to be displayed properly with dynamic colors.

All illustrations and multi-tonal icons require the svg paths to be tagged with the `name` attribute in order for each individual path to be tinted the appropriate color. Any untagged path will not be tinted and the resulting image will be incorrect.

The supported tags are as follows:

* outline
* primary
* secondary
* tertiary
* accent
* logo
* navigation
* navigationActiveAccent

# FOTTO

Generates beautiful photo book PDFs based on templates and album files.

## File formats

### Templates

```json
{
  // default style used for placeholders that do not specify a specific style
  "defaultStyleRef": "default", 

  // margins for entire album
  "margins": {
    "inner": 6,
    "top": 9,
    "outer": 12,
    "bottom": 18
  },
  
  // list of page templates that can be refrenced by name, e.g. a title page
  // a grid of 2x2 images, etc.
  "pageTemplates": {
    "{your page template identifier}": {
      // a list of placeholders on this page
      "placeholders": {
        "{your placeholder identifier}": {
        "{your placeholder identifier}": {d
          // will this be assigned an image or text?
          "contentType": "text|image",
          // for ordering
          "layer": 1,
          // coordinates, in percent
          "x":10, "y": 50, "width":80, "height": 10,
          // references a style defined below
          "styleRef": "{a style identifier}",
          // References another placeholder. If the album does not specify content for this placeholder,
          // it will look at the specified default instead. This is useful, for instance, if you
          // want to create a background based on a foreground image, probably with a different style
          "defaultTo": "{another placeholder id}"
        },
        // more placeholders
        ...
      }
    },
    // more page templates
    ...
  },
  
  // pre-defined styles that can be used for all placeholders
  "styles": {
    "{your style identifier": {
      // undefined properties will be inhereted from parent style
      "parentRef": "{another style identifier}",
      "opacity": 0.5,
      "color": "#rrggbb",
      "fontSize": 9,
      "verticalAlign": "bottom|center|top"
      "parentRef": "text",
      "fontWeight":"bold|normal",
      "textAlign":"center|justify",
      "borderWidth": 5,
      "borderColor": "#rrggbb"
    },
    // more styles
    ...
  }
}
```

### Albums

```json
{
  // the template file to use for this album
  "templateFile": "template.json",
  // language and country for hyphenation
  "language": "de",
  "country": "de",
  // one entry per page
  "pages": [
    {
      // references a page template in the template file
      "pageTemplateRef": "{a page template identifier}",
        // each element identifies a placeholder in the template file
      "assignments": {
        // in case the placeholders type is 'image', reference a file
        "{a placeholder identifier}":"img/IMG_1100.JPG",
        // in case the placeholders type is 'text', the tex
        "{another placeholder identifier}": "Arbitrary text"
      },
      // you can override styles, per placeholder
      "styleOverrides": {
        // note we reference placeholders here, not styles
        "{a placeholder identifier}": {
          "borderColor": "#rrggbb"
        }
      }
    },
    // more pages
    ...
  ]
}
```

## Command line options

```
fotto
    --width UNIT    The page with, in cm, in, or pt
    --page UNIT     The page height, in cm, in, or pt
    --watch true    If set, watch the input file and re-render the output on every change.
    --out FILE      Output file
    --pages RANGE   Render only a subset of pages. Useful in combination with --watch for performance reasons.
    FILE            Input album file
#!/usr/bin/env python3
"""Generates the CloakRun MapLibre styles from one layer definition and two palettes.

    python scripts/map/gen_styles.py

Writes core/map/src/main/assets/styles/cloakrun-night.json and cloakrun-day.json.

Data: OpenFreeMap vector tiles (OpenMapTiles schema v3.16) through the TileJSON at
https://tiles.openfreemap.org/planet. Glyphs come from the same host; only "Noto Sans Regular",
"Noto Sans Bold" and "Noto Sans Italic" exist there, one font per text-font array (see DECISIONS D-009).

Design notes:
- Tunnels are not drawn: players cannot use them, and they clutter the chase view.
- Footpaths, pedestrian streets and steps are emphasised. They are where hiders run and where
  cache points may spawn.
- Buildings extrude from zoom 14.5 and their colour brightens with height ("height glow").
  Extrusion height eases in with zoom, so buildings rise during the match-start fly-in.
"""
from __future__ import annotations

import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "core" / "map" / "src" / "main" / "assets" / "styles"

SOURCE = "openmaptiles"
GLYPHS = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"
TILEJSON = "https://tiles.openfreemap.org/planet"
REGULAR = ["Noto Sans Regular"]
BOLD = ["Noto Sans Bold"]
ITALIC = ["Noto Sans Italic"]

NIGHT = {
    "background": "#07061A",
    "wood": "#0C1F24",
    "grass": "#0D2324",
    "park": "#0F2A27",
    "park_edge": "#3DF5B0",
    "pitch": "#122E2A",
    "restricted": "#2A1430",
    "water": "#0A1433",
    "water_edge": "#5CF2FF",
    "waterway": "#1B3A6B",
    "path": "#9D8CFF",
    "path_glow": "#7C5CFF",
    "service": "#1E1A45",
    "minor": "#2C2566",
    "secondary": "#6B55E8",
    "secondary_glow": "#7C5CFF",
    "primary": "#5CF2FF",
    "primary_glow": "#19C8DE",
    "motorway": "#C9FBFF",
    "motorway_glow": "#5CF2FF",
    "rail": "#3F3A6E",
    "building_low": "#120D33",
    "building_mid": "#22195C",
    "building_high": "#4532C9",
    "building_flat": "#130F36",
    "label_place": "#E4DDFF",
    "label_suburb": "#A48CFF",
    "label_road": "#9C90E6",
    "label_water": "#5C9CD6",
    "label_poi": "#8F84D8",
    "halo": "#07061A",
    "glow_opacity": 0.38,
    "extrusion_opacity": 0.88,
}

DAY = {
    "background": "#EEF0F8",
    "wood": "#CDE7D5",
    "grass": "#DBEFE0",
    "park": "#D2EDDB",
    "park_edge": "#41B98A",
    "pitch": "#CBE8D6",
    "restricted": "#E9DCE6",
    "water": "#B7D4F2",
    "water_edge": "#6FA9DE",
    "waterway": "#9CC3EC",
    "path": "#7C5CFF",
    "path_glow": "#A48CFF",
    "service": "#FFFFFF",
    "minor": "#FFFFFF",
    "secondary": "#FFFFFF",
    "secondary_glow": "#B9AEF5",
    "primary": "#E3DCFF",
    "primary_glow": "#8F7BF0",
    "motorway": "#C9F3F8",
    "motorway_glow": "#2BB3C9",
    "rail": "#B4B2C9",
    "building_low": "#E2E3F1",
    "building_mid": "#D6D6EC",
    "building_high": "#C3BEEA",
    "building_flat": "#E1E2F0",
    "label_place": "#1D1840",
    "label_suburb": "#5A41E0",
    "label_road": "#4B4675",
    "label_water": "#3F6FA6",
    "label_poi": "#5E5A80",
    "halo": "#FFFFFF",
    "glow_opacity": 0.55,
    "extrusion_opacity": 0.8,
}

NOT_TUNNEL = ["!=", ["get", "brunnel"], "tunnel"]

# OpenMapTiles landuse classes treated as not publicly accessible (see DECISIONS D-029).
RESTRICTED_LANDUSE = ["military", "industrial", "quarry", "railway", "garages", "dam", "school", "kindergarten"]


def zoom_width(*stops: float) -> list:
    """Exponential width ramp, e.g. zoom_width(12, 0.5, 18, 8)."""
    expr: list = ["interpolate", ["exponential", 1.5], ["zoom"]]
    expr.extend(stops)
    return expr


def scaled(stops: tuple, k: float) -> list:
    """A zoom ramp whose widths are multiplied by k (zoom expressions cannot be nested in arithmetic)."""
    out = []
    for i in range(0, len(stops), 2):
        out += [stops[i], round(stops[i + 1] * k, 3)]
    return zoom_width(*out)


def road(layer_id: str, classes: list[str], stops: tuple, color: str, glow: str | None, p: dict,
         glow_scale: float = 4.0, minzoom: float = 5, dash: list | None = None) -> list[dict]:
    filt = ["all", ["in", ["get", "class"], ["literal", classes]], NOT_TUNNEL]
    width = zoom_width(*stops)
    layers = []
    if glow:
        glow_width = scaled(stops, glow_scale)
        layers.append({
            "id": f"{layer_id}-glow",
            "type": "line",
            "source": SOURCE,
            "source-layer": "transportation",
            "minzoom": minzoom,
            "filter": filt,
            "layout": {"line-cap": "round", "line-join": "round"},
            "paint": {
                "line-color": glow,
                "line-width": glow_width,
                "line-blur": scaled(stops, glow_scale * 0.8),
                "line-opacity": p["glow_opacity"],
            },
        })
    core = {
        "id": layer_id,
        "type": "line",
        "source": SOURCE,
        "source-layer": "transportation",
        "minzoom": minzoom,
        "filter": filt,
        "layout": {"line-cap": "round", "line-join": "round"},
        "paint": {"line-color": color, "line-width": width},
    }
    if dash:
        core["layout"]["line-cap"] = "butt"
        core["paint"]["line-dasharray"] = dash
    layers.append(core)
    return layers


def build(name: str, p: dict) -> dict:
    layers: list[dict] = [
        {"id": "background", "type": "background", "paint": {"background-color": p["background"]}},
        {
            "id": "landcover-wood", "type": "fill", "source": SOURCE, "source-layer": "landcover",
            "filter": ["==", ["get", "class"], "wood"],
            "paint": {"fill-color": p["wood"], "fill-antialias": True},
        },
        {
            "id": "landcover-grass", "type": "fill", "source": SOURCE, "source-layer": "landcover",
            "filter": ["in", ["get", "class"], ["literal", ["grass", "farmland", "wetland"]]],
            "paint": {"fill-color": p["grass"]},
        },
        {
            "id": "park", "type": "fill", "source": SOURCE, "source-layer": "park",
            "paint": {"fill-color": p["park"]},
        },
        {
            "id": "park-edge", "type": "line", "source": SOURCE, "source-layer": "park", "minzoom": 12,
            "paint": {
                "line-color": p["park_edge"],
                "line-opacity": 0.28,
                "line-width": zoom_width(12, 0.5, 18, 3),
                "line-blur": zoom_width(12, 0.5, 18, 4),
            },
        },
        {
            "id": "landuse-leisure", "type": "fill", "source": SOURCE, "source-layer": "landuse", "minzoom": 12,
            "filter": ["in", ["get", "class"], ["literal", ["pitch", "playground", "stadium", "track", "cemetery", "garden"]]],
            "paint": {"fill-color": p["pitch"]},
        },
        {
            # Land that is rarely public. Rendered faintly so the safety pass can query it.
            "id": "landuse-restricted", "type": "fill", "source": SOURCE, "source-layer": "landuse", "minzoom": 12,
            "filter": ["in", ["get", "class"], ["literal", RESTRICTED_LANDUSE]],
            "paint": {"fill-color": p["restricted"], "fill-opacity": 0.55},
        },
        {
            "id": "water", "type": "fill", "source": SOURCE, "source-layer": "water",
            "filter": NOT_TUNNEL,
            "paint": {"fill-color": p["water"]},
        },
        {
            "id": "water-edge", "type": "line", "source": SOURCE, "source-layer": "water", "minzoom": 11,
            "filter": NOT_TUNNEL,
            "paint": {
                "line-color": p["water_edge"],
                "line-opacity": 0.35,
                "line-width": zoom_width(11, 0.6, 18, 4),
                "line-blur": zoom_width(11, 1, 18, 6),
            },
        },
        {
            "id": "waterway", "type": "line", "source": SOURCE, "source-layer": "waterway",
            "filter": NOT_TUNNEL,
            "paint": {"line-color": p["waterway"], "line-width": zoom_width(10, 0.5, 18, 6)},
        },
        {
            "id": "building-flat", "type": "fill", "source": SOURCE, "source-layer": "building",
            "minzoom": 13, "maxzoom": 14.5,
            "paint": {"fill-color": p["building_flat"], "fill-opacity": ["interpolate", ["linear"], ["zoom"], 13, 0, 14, 1]},
        },
    ]
    layers += road("road-rail", ["rail", "transit"], (12, 0.6, 18, 2.4), p["rail"], None, p,
                   minzoom=12, dash=[2, 2])
    layers += road("road-service", ["service", "track"], (14, 0.4, 18, 4), p["service"], None, p, minzoom=14)
    layers += road("road-minor", ["minor"], (12, 0.4, 18, 9), p["minor"], None, p, minzoom=12)
    layers += road("road-secondary", ["secondary", "tertiary"], (10, 0.6, 18, 12), p["secondary"],
                   p["secondary_glow"], p, glow_scale=2.6, minzoom=9)
    layers += road("road-primary", ["primary", "trunk"], (8, 0.6, 18, 13), p["primary"],
                   p["primary_glow"], p, glow_scale=3.0, minzoom=7)
    layers += road("road-motorway", ["motorway"], (6, 0.6, 18, 15), p["motorway"],
                   p["motorway_glow"], p, glow_scale=3.2, minzoom=5)
    layers += road("road-path", ["path"], (14, 0.7, 18, 2.6), p["path"], p["path_glow"], p,
                   glow_scale=3.5, minzoom=14, dash=[1.6, 1.2])
    layers += [
        {
            "id": "building-3d", "type": "fill-extrusion", "source": SOURCE, "source-layer": "building",
            "minzoom": 14.5,
            "filter": ["!=", ["get", "hide_3d"], True],
            "paint": {
                "fill-extrusion-color": [
                    "interpolate", ["linear"], ["coalesce", ["get", "render_height"], 6],
                    0, p["building_low"], 18, p["building_mid"], 60, p["building_high"],
                ],
                "fill-extrusion-height": [
                    "interpolate", ["linear"], ["zoom"],
                    14.5, 0,
                    15.5, ["coalesce", ["get", "render_height"], 6],
                ],
                "fill-extrusion-base": ["coalesce", ["get", "render_min_height"], 0],
                "fill-extrusion-opacity": p["extrusion_opacity"],
                "fill-extrusion-vertical-gradient": True,
            },
        },
        {
            "id": "label-water", "type": "symbol", "source": SOURCE, "source-layer": "water_name", "minzoom": 12,
            "layout": {
                "text-field": ["get", "name"], "text-font": ITALIC, "text-size": 12,
                "text-letter-spacing": 0.1, "symbol-placement": "point",
            },
            "paint": {"text-color": p["label_water"], "text-halo-color": p["halo"], "text-halo-width": 1.2},
        },
        {
            "id": "label-road", "type": "symbol", "source": SOURCE, "source-layer": "transportation_name", "minzoom": 14,
            "filter": NOT_TUNNEL,
            "layout": {
                "text-field": ["get", "name"], "text-font": REGULAR,
                "text-size": ["interpolate", ["linear"], ["zoom"], 14, 10, 18, 13],
                "symbol-placement": "line", "text-max-angle": 30, "text-padding": 4,
            },
            "paint": {"text-color": p["label_road"], "text-halo-color": p["halo"], "text-halo-width": 1.4},
        },
        {
            "id": "label-poi", "type": "symbol", "source": SOURCE, "source-layer": "poi", "minzoom": 16,
            "filter": ["<=", ["coalesce", ["get", "rank"], 99], 12],
            "layout": {"text-field": ["get", "name"], "text-font": REGULAR, "text-size": 11, "text-max-width": 8},
            "paint": {"text-color": p["label_poi"], "text-halo-color": p["halo"], "text-halo-width": 1.2},
        },
        {
            "id": "label-suburb", "type": "symbol", "source": SOURCE, "source-layer": "place", "minzoom": 11, "maxzoom": 16,
            "filter": ["in", ["get", "class"], ["literal", ["suburb", "quarter", "neighbourhood"]]],
            "layout": {
                "text-field": ["upcase", ["get", "name"]], "text-font": BOLD, "text-size": 11,
                "text-letter-spacing": 0.22, "text-max-width": 8,
            },
            "paint": {"text-color": p["label_suburb"], "text-halo-color": p["halo"], "text-halo-width": 1.4},
        },
        {
            "id": "label-place", "type": "symbol", "source": SOURCE, "source-layer": "place", "maxzoom": 14,
            "filter": ["in", ["get", "class"], ["literal", ["city", "town", "village"]]],
            "layout": {
                "text-field": ["get", "name"], "text-font": BOLD,
                "text-size": ["interpolate", ["linear"], ["zoom"], 6, 12, 13, 18],
            },
            "paint": {"text-color": p["label_place"], "text-halo-color": p["halo"], "text-halo-width": 1.6},
        },
    ]
    return {
        "version": 8,
        "name": name,
        "metadata": {"cloakrun:generator": "scripts/map/gen_styles.py"},
        "sources": {SOURCE: {"type": "vector", "url": TILEJSON}},
        "glyphs": GLYPHS,
        "layers": layers,
    }


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for file, name, palette in (("cloakrun-night.json", "CloakRun Night", NIGHT), ("cloakrun-day.json", "CloakRun Day", DAY)):
        path = OUT / file
        path.write_text(json.dumps(build(name, palette), indent=1) + "\n", encoding="utf-8", newline="\n")
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

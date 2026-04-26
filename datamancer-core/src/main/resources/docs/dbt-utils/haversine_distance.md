---
title: "haversine_distance"
sidebar_label: "haversine_distance"
id: "haversine_distance"
description: "This macro calculates the [haversine distance](https://en.wikipedia.org/wiki/Haversine_formula) between a pair of x/y coordinates."
category: "SQL generators"
---

This macro calculates the [haversine distance](https://en.wikipedia.org/wiki/Haversine_formula) between a pair of x/y coordinates.

Optionally takes a `unit` string argument ('km' or 'mi') which defaults to miles (imperial system).

**Usage:**

```sql
{{ dbt_utils.haversine_distance(48.864716, 2.349014, 52.379189, 4.899431) }}

{{ dbt_utils.haversine_distance(
    lat1=48.864716,
    lon1=2.349014,
    lat2=52.379189,
    lon2=4.899431,
    unit='km'
) }}
```

__Args__:

 * `lat1` (required): latitude of first location
 * `lon1` (required): longitude of first location
 * `lat2` (required): latitude of second location
 * `lon3` (required): longitude of second location
 * `unit` (optional, default=`'mi'`): one of `mi` (miles) or `km` (kilometers)

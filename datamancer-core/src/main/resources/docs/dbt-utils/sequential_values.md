---
title: "sequential_values"
sidebar_label: "sequential_values"
id: "sequential_values"
description: "This test confirms that a column contains sequential values. It can be used for both numeric values, and datetime values, as follows:"
category: "Generic Tests"
---

This test confirms that a column contains sequential values. It can be used
for both numeric values, and datetime values, as follows:

```yaml
version: 2

seeds:
  - name: util_even_numbers
    columns:
      - name: i
        tests:
          - dbt_utils.sequential_values:
              arguments:
                interval: 2


  - name: util_hours
    columns:
      - name: date_hour
        tests:
          - dbt_utils.sequential_values:
              arguments:
                interval: 1
                datepart: 'hour'
```

__Args__:

 * `interval` (default=1): The gap between two sequential values
 * `datepart` (default=None): Used when the gaps are a unit of time. If omitted, the test will check for a numeric gap.

This test supports the `group_by_columns` parameter; see [Grouping in tests](#grouping-in-tests) for details.

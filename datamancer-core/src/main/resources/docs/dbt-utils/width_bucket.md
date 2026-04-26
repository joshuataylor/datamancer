---
title: "width_bucket"
sidebar_label: "width_bucket"
id: "width_bucket"
description: "This macro is modeled after the `width_bucket` function natively available in Snowflake."
category: "SQL generators"
---

This macro is modeled after the `width_bucket` function natively available in Snowflake.

From the original Snowflake [documentation](https://docs.snowflake.net/manuals/sql-reference/functions/width_bucket.html):

Constructs equi-width histograms, in which the histogram range is divided into intervals of identical size, and returns the bucket number into which the value of an expression falls, after it has been evaluated. The function returns an integer value or null (if any input is null).
Notes:

__Args__:

 * `expr`: The expression for which the histogram is created. This expression must evaluate to a numeric value or to a value that can be implicitly converted to a numeric value.

 * `min_value` and `max_value`: The low and high end points of the acceptable range for the expression. The end points must also evaluate to numeric values and not be equal.

 * `num_buckets`:  The desired number of buckets; must be a positive integer value. A value from the expression is assigned to each bucket, and the function then returns the corresponding bucket number.

When an expression falls outside the range, the function returns:

 * `0` if the expression is less than min_value.
 * `num_buckets + 1` if the expression is greater than or equal to max_value.

**Usage:**

```sql
{{ dbt_utils.width_bucket(expr, min_value, max_value, num_buckets) }}
```

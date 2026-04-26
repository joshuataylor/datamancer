---
title: "get_filtered_columns_in_relation"
sidebar_label: "get_filtered_columns_in_relation"
id: "get_filtered_columns_in_relation"
description: "This macro returns an iterable Jinja list of columns for a given [relation](https://docs.getdbt.com/docs/writing-code-in-dbt/class-reference/#relation), (i.e. not from a CTE)"
category: "Introspective macros"
---

This macro returns an iterable Jinja list of columns for a given [relation](https://docs.getdbt.com/docs/writing-code-in-dbt/class-reference/#relation), (i.e. not from a CTE)

- optionally exclude columns
- the input values are not case-sensitive (input uppercase or lowercase and it will work!)

> Note: The native [`adapter.get_columns_in_relation` macro](https://docs.getdbt.com/reference/dbt-jinja-functions/adapter#get_columns_in_relation) allows you
to pull column names in a non-filtered fashion, also bringing along with it other (potentially unwanted) information, such as dtype, char_size, numeric_precision, etc.

__Args__:

 * `from` (required): a [Relation](https://docs.getdbt.com/reference/dbt-classes#relation) (a `ref` or `source`) that contains the list of columns you wish to select from
 * `except` (optional, default=`[]`): The name of the columns you wish to exclude. (case-insensitive)

**Usage:**

```sql
-- Returns a list of the columns from a relation, so you can then iterate in a for loop
{% set column_names = dbt_utils.get_filtered_columns_in_relation(from=ref('your_model'), except=["field_1", "field_2"]) %}
...
{% for column_name in column_names %}
    max({{ column_name }}) ... as max_'{{ column_name }}',
{% endfor %}
...
```

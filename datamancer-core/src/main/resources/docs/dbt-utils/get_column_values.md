---
title: "get_column_values"
sidebar_label: "get_column_values"
id: "get_column_values"
description: "This macro returns the unique values for a column in a given [relation](https://docs.getdbt.com/docs/writing-code-in-dbt/class-reference/#relation) as an array."
category: "Introspective macros"
---

This macro returns the unique values for a column in a given [relation](https://docs.getdbt.com/docs/writing-code-in-dbt/class-reference/#relation) as an array.

__Args__:

 * `table` (required): a [Relation](https://docs.getdbt.com/reference/dbt-classes#relation) (a `ref` or `source`) that contains the list of columns you wish to select from
 * `column` (required): The name of the column you wish to find the column values of
 * `where` (optional, default=`none`): A where clause to filter the column values by.
 * `order_by` (optional, default=`'count(*) desc'`): How the results should be ordered. Must be an aggregate function, i.e. count(*), max(sort_order). The default is to order by `count(*) desc`, i.e. decreasing frequency. Setting this as `'my_column'` will sort alphabetically, while `'min(created_at)'` will sort by when the value was first observed.
 * `max_records` (optional, default=`none`): The maximum number of column values you want to return
 * `default` (optional, default=`[]`): The results this macro should return if the relation has not yet been created (and therefore has no column values).

**Usage:**

```sql
-- Returns a list of the payment_methods in the stg_payments model_
{% set payment_methods = dbt_utils.get_column_values(table=ref('stg_payments'), column='payment_method') %}

{% for payment_method in payment_methods %}
    ...
{% endfor %}

...
```

```sql
-- Returns the list sorted alphabetically
{% set payment_methods = dbt_utils.get_column_values(
        table=ref('stg_payments'),
        where="payment_method = 'bank_transfer'",
        column='payment_method',
        order_by='payment_method'
) %}
```

```sql
-- Returns the list sorted my most recently observed
{% set payment_methods = dbt_utils.get_column_values(
        table=ref('stg_payments'),
        column='payment_method',
        order_by='max(created_at) desc',
        max_records=50,
        default=['bank_transfer', 'coupon', 'credit_card']
%}
...
```

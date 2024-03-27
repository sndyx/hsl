Docs / Getting Started / [Advanced Syntax](advanced-syntax.md)

---

# Advanced Syntax

This is a collection of advanced syntax elements with examples. You can
 find more examples
 [here](https://github.com/sndyx/hsl/tree/master/examples).

## Complex Equations

Equations can be naturally typed when not in `strict` mode.

```rust
exp += (kills / 5) * 20
```

Constant equations are evaluated during compilation.

```rust
x += 16 * 4
  // 64
```

Order of operations are respected for all equations.

## Complex Functions

A function with two parameters and a return expression.

```rust
fn print_sum(_a, _b) {
    _result = _a + _b
    send_message("sum of ${_a} and ${_b} is ${_result}")
}
```

---

Next: [Macros](macros.md)
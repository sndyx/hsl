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
fn sum(_a, _b) {
    return _a + _b
}

fn print_sum() {
    message("sum of ${a} and ${b} is ${sum(a, b)}") 
}
```

## Match Expression

```rust
match (x) {
    1 => message("x is 1")
    2 => message("x is 2")
    else => {
        message("x is neither")
    }
}
```

## Ranges

Check if a number is within a range using `in` operator.

```rust
x = 10
y = 9
if (x in 1..(y + 1)) {
    message("fits in range")
}
```

## Consts

```rust
const x = 5
```

Consts can only be declared in the global scope, and will never change.

## Enums

```rust
enum Color {
    Red = 1
    Blue = 2
    Green = 3
}
```

```rust
x = Color.Red
// x = 1
```

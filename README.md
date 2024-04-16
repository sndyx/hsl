# HSL

HSL (Housing Scripting Language) is a programming language that provides
a near 1:1 mapping of housing actions while also maintaining many optional
abstractions and syntax sugar.

## Table of contents

<!--- TOC -->

- [Installation](#installation)
- [Language Documentation](#language-documentation)
- [Compiler](#compiler)
- [Developers](#developers)
    - [Compiler](#hsc-housing-script-compiler)
        - [Compiling](#compiling)
    - [Standard Library](#standard-library)
- [Credits](#credits)
- [License](#license)

<!--- END -->

## Installation

You can download the latest release executables [here](https://github.com/sndyx/hsl/releases).

Now, just add both to the `PATH` variable.

## Language Documentation

Documentation can be found [here](/docs/getting-started/creating-a-project.md).

## Compiler

TODO

## Developers

This repository is divided into a few major sections.

### HSC (Housing Script Compiler)

The compiler, is located in the `compiler` directory. The main entry point
for the compiler is `Driver.kt`. The lexer and parser are both located in
the `com.hsc.compiler.parse` namespace. The resultant AST (defined in
`com.hsc.compiler.ir` along with the `Action` IR) is then lowered through
a series of passes in `com.hsc.compiler.codegen`, desugaring and validating
the AST. Finally, the AST is converted to Actions via a single
`AstToActionTransformer` pass.

#### Compiling

Compile the compiler with gradle: `./gradlew compiler:build`

### Standard Library

The standard library is located in the `std` directory. It is designed to
be a minimal companion to the built-in functions provided by Housing.

# Credits

This project is only possible due to the hard work put in by members of
the Housing community!

Special thanks to [HousingEditor](https://github.com/ImaDoofus/HousingEditor)
 and [HousingDevs](https://github.com/housingdevs) for the well-established
 foundations and action schemas.

# License

HSC, Mason are distributed under the terms of the MIT license. See
[LICENSE.txt](LICENSE.txt) for the full license.

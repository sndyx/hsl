Docs / Getting Started / [Creating a Project](creating-a-project.md)

---

# Creating a Project

## Initialization

After HSC and Mason are installed, you can create a project. To begin,
 create a folder for your project and run `mason init`. This will create
 the necessary files for your project. Your directory should now look
 like this:

```
example
├── House.toml
└── src
    └── example.hsl
```

Source files are located in the `src` directory and end in the file
 extension `.hsl`. These files are where you write your functions. A
 single source file can contain any amount of functions, and how you
 structure and allocate functions to files is up to you. They do not
 affect the output of the compiler.

`House.toml` contains the [manifest](../reference/manifest.md) information
 which controls the behavior of the compiler as well as features and
 dependencies of your project.

## Building

To build your project, run `mason build`. The resultant actions will be
 located in `build/actions.json`

---

Next: [Basic Syntax](basic-syntax.md)
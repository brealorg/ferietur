# BUGFIX02R1 installer correction

Failure class: bootstrap/target path collision.

Original:
- SCRIPT_DIR: `~/Downloads/ferietur01-bugfix02-back-rotation`
- TARGET:     `~/Downloads/ferietur01-bugfix02-back-rotation`

The destructive successor setup therefore removed the bootstrap itself before
reading payload/patcher resources.

R1:
- package/bootstrap root: `ferietur01-bugfix02r1-bootstrap`
- successor target:       `ferietur01-bugfix02-back-rotation`
- adds `realpath -m` collision detection
- verifies payload, patched UI and Gradle patcher before target deletion

No product-code change from BUGFIX02.

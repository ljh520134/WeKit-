fn main() {
    println!("cargo:rustc-link-lib=log");
    println!("cargo:rustc-link-lib=dl");
    println!("cargo:rustc-link-lib=unwind");
}

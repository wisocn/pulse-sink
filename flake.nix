{
  description = "A flake that provides just and aws-cli";

  inputs = {
    nixpkgs.url = "nixpkgs";  # Fetches the latest stable Nixpkgs
    flake-utils.url = "github:numtide/flake-utils"; 
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system; 
        };
      in {
        packages.default = pkgs.just; 

        devShells.default = pkgs.mkShell {
          buildInputs = [
            pkgs.just       
            pkgs.awscli
          ];
        };
      }
    );
}

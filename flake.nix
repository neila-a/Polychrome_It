{
    inputs = {
        nixpkgs.url = "git+https://mirror.nju.edu.cn/git/nixpkgs.git?ref=nixos-unstable&shallow=1";
        xmcl = {
            url = "https://proxy.gitwarp.top/https://github.com/MCSeekeri/NUR/archive/refs/heads/master.zip";
            flake = false;
        };

        #flake-utils.url = "github:numtide/flake-utils";
        flake-utils.url = "https://proxy.gitwarp.top/https://github.com/numtide/flake-utils/archive/refs/heads/master.zip";
    };
    description = "Make your Minecraft more colorful.\nSupports full RGB range.";
    outputs =
        {
            self,
            nixpkgs,
            flake-utils,
            xmcl
        }:
        flake-utils.lib.eachDefaultSystem (
            system:
            let
                pkgs = import nixpkgs {
                    inherit system;
                };
                runtimeLibs = (pkgs.callPackage "${xmcl}/pkgs/xmcl/common.nix" {}).runtimeLibs;
                allLibs = pkgs.electron.buildInputs ++ [
                    pkgs.stdenv.cc.cc.lib
                ] ++ pkgs.lib.optionals pkgs.stdenv.hostPlatform.isLinux runtimeLibs;
            in
            {
                devShells.default = pkgs.mkShell {
                    packages = allLibs;
                    LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath allLibs;
                };
            }
        );
}

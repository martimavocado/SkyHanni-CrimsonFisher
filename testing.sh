rm ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/dev/.minecraft/mods/SkyHanni*
mv build/libs/SkyHanni* ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/dev/.minecraft/mods
flatpak run org.prismlauncher.PrismLauncher -l dev

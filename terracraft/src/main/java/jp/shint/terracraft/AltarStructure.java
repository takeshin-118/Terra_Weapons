package jp.shint.terracraft;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Default nether-axe altar definition. Rotation is accepted; mirroring is deliberately not. */
public final class AltarStructure {
    private AltarStructure() { }
    public static boolean matchesNetherAxe(World world, BlockPos core) {
        return missingMask(world, core) == 0;
    }
    /** One bit per floor cell; the rotation with the fewest missing blocks is shown to the player. */
    public static int missingMask(World world, BlockPos core) {
        int bestMask = -1;
        for (int rotation = 0; rotation < 4; rotation++) {
            int mask = 0;
            TerracraftConfig.AltarDefinition definition = TerracraftConfig.altar();
            for (int row = 0; row < 5; row++) for (int column = 0; column < 5; column++) {
                int x = column - 2, z = row - 2;
                BlockPos pos = rotate(core.down(), x, z, rotation);
                if (!matches(definition, definition.layers()[0][row].charAt(column), world.getBlockState(pos))) {
                    mask |= 1 << (row * 5 + column);
                }
            }
            if (bestMask == -1 || Integer.bitCount(mask) < Integer.bitCount(bestMask)) bestMask = mask;
        }
        return bestMask;
    }
    private static boolean matches(World world, BlockPos core, int rotation) {
        TerracraftConfig.AltarDefinition definition = TerracraftConfig.altar();
        for (int row = 0; row < 5; row++) for (int column = 0; column < 5; column++) {
            int x = column - 2, z = row - 2;
            BlockPos pos = rotate(core.down(), x, z, rotation);
            if (!matches(definition, definition.layers()[0][row].charAt(column), world.getBlockState(pos))) return false;
        }
        // Upper layer is also evaluated from the external character diagram.
        for (int row = 0; row < 5; row++) for (int column = 0; column < 5; column++) {
            BlockPos pos = rotate(core, column - 2, row - 2, rotation);
            if (!matches(definition, definition.layers()[1][row].charAt(column), world.getBlockState(pos))) return false;
        }
        return true;
    }
    private static BlockPos rotate(BlockPos origin, int x, int z, int r) {
        return switch (r) { case 1 -> origin.add(-z, 0, x); case 2 -> origin.add(-x, 0, -z); case 3 -> origin.add(z, 0, -x); default -> origin.add(x, 0, z); };
    }
    private static boolean matches(TerracraftConfig.AltarDefinition definition, char symbol, BlockState state) {
        return symbol == '.' ? state.isAir() : definition.symbols().get(symbol).test(state);
    }
}

package carcassonne.model.ai;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import carcassonne.model.Player;
import carcassonne.model.grid.Grid;
import carcassonne.model.tile.Tile;
import carcassonne.model.tile.TileStack;
import carcassonne.settings.GameSettings;

public class RuleBasedAI implements ArtificialIntelligence {
    private static final int VALUE_THRESHOLD = 0;
    private static final int FIELD_VALUE_THRESHOLD = 6;
    private static final String EMPTY_COLLECTION = "Cannot choose random element from empty collection!";
    private final GameSettings settings;
    private Optional<CarcassonneMove> currentMove;

    public RuleBasedAI(GameSettings settings) {
        this.settings = settings;
    }

    @Override
    public Optional<CarcassonneMove> calculateBestMoveFor(Collection<Tile> tiles, Player player, Grid grid, TileStack stack) {
        currentMove = Optional.empty();
        Collection<CarcassonneMove> possibleMoves = new ArrayList<>();
        for (Tile tile : tiles) {
            possibleMoves.addAll(grid.getPossibleMoves(tile, player, settings));
        }
        // RULE 1: Only consider move with a positive value:
        List<CarcassonneMove> consideredMoves = possibleMoves.stream().filter(it -> it.getValue() >= VALUE_THRESHOLD).collect(toList());
        // RULE 2: Do not place last meeple on fields
        if (player.getFreeMeeples() == 1 && stack.getSize() > settings.getAmountOfPlayers()) {
            consideredMoves = consideredMoves.stream().filter(it -> !it.isFieldMove()).collect(toList());
        }
        // RULE 3: Do not place low value fields
        consideredMoves = consideredMoves.stream().filter(it -> !isLowValueFieldMove(it)).collect(toList());
        if (!consideredMoves.isEmpty()) {
            currentMove = chooseBestMove(consideredMoves);
        }
        System.out.println(currentMove); // TODO (HIGH) remove debug output
        return currentMove;
    }

    @Override
    public Tile chooseTileToDrop(Collection<Tile> tiles) {
        return chooseRandom(tiles); // TODO (HIGH) find a meaningful heuristic
    }

    @Override
    public Optional<CarcassonneMove> getCurrentMove() {
        return currentMove;
    }

    private Optional<CarcassonneMove> chooseBestMove(List<CarcassonneMove> consideredMoves) {
        double maximumValue = consideredMoves.stream().mapToDouble(it -> it.getValue()).max().getAsDouble();
        Stream<CarcassonneMove> bestMoves = consideredMoves.stream().filter(it -> it.getValue() == maximumValue);
        return chooseAmongBestMoves(bestMoves);
    }

    private Optional<CarcassonneMove> chooseAmongBestMoves(Stream<CarcassonneMove> bestMoves) {
        RuleBasedComparator comparator = new RuleBasedComparator();
        List<CarcassonneMove> listOfMoves = bestMoves.collect(toList());
        Collections.sort(listOfMoves, comparator.reversed());
        CarcassonneMove first = listOfMoves.get(0);
        return Optional.of(chooseRandom(listOfMoves.stream().filter(it -> comparator.compare(it, first) == 0).collect(toList())));
    }

    private <T> T chooseRandom(Collection<T> elements) {
        Optional<T> randomElement = elements.stream().skip(new Random().nextInt(elements.size())).findFirst();
        return randomElement.orElseThrow(() -> new IllegalArgumentException(EMPTY_COLLECTION));
    }

    private boolean isLowValueFieldMove(CarcassonneMove move) {
        return move.isFieldMove() && move.getValue() < FIELD_VALUE_THRESHOLD;
    }

}

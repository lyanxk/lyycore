package org.lyy.lyycore.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import java.util.*;

/** Immutable experiment data with its signature and target graph compiled once. */
public final class ExperimentDefinition {
    public enum Element implements StringRepresentable {
        ICE(0xFF63B6DF), FIRE(0xFFE47770), LIGHTNING(0xFFDDB54B);
        public final int color;
        Element(int color) { this.color = color; }
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    public record Node(Element element, int cell) {
        public static final Codec<Node> CODEC = RecordCodecBuilder.create(i -> i.group(
                StringRepresentable.fromEnum(Element::values).fieldOf("element").forGetter(Node::element),
                Codec.intRange(0, 24).fieldOf("cell").forGetter(Node::cell)).apply(i, Node::new));
    }
    public record Link(int from, int to) {
        public static final Codec<Link> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 24).fieldOf("from").forGetter(Link::from),
                Codec.intRange(0, 24).fieldOf("to").forGetter(Link::to)).apply(i, Link::new));
    }
    public static final Codec<ExperimentDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Node.CODEC.listOf().fieldOf("nodes").forGetter(ExperimentDefinition::nodes),
            Link.CODEC.listOf().fieldOf("links").forGetter(ExperimentDefinition::links),
            Codec.intRange(0, 24).listOf().fieldOf("target").forGetter(ExperimentDefinition::target)
    ).apply(i, ExperimentDefinition::new));

    private final List<Node> nodes;
    private final List<Link> links;
    private final List<Integer> target;
    private final int[] initial, targetCells;
    private final String signature;
    private final Set<String> targetGraph;

    public ExperimentDefinition(List<Node> nodes, List<Link> links, List<Integer> target) {
        this.nodes = List.copyOf(nodes); this.links = List.copyOf(links); this.target = List.copyOf(target);
        initial = this.nodes.stream().mapToInt(Node::cell).toArray();
        targetCells = this.target.stream().mapToInt(Integer::intValue).toArray();
        if (this.nodes.isEmpty() || this.nodes.size() > 25 || this.links.isEmpty() || this.links.size() > 300
                || !validCells(initial, this.nodes.size()) || !validCells(targetCells, this.nodes.size()))
            throw new IllegalArgumentException("Invalid experiment graph");
        for (var node : this.nodes) Objects.requireNonNull(node.element, "Experiment element");
        for (var link : this.links) if (link.from < 0 || link.to < 0 || link.from >= this.nodes.size() || link.to >= this.nodes.size() || link.from == link.to)
            throw new IllegalArgumentException("Invalid experiment link");
        // Preserve the exact old signature format so saved report positions remain valid.
        signature = this.nodes + "/" + this.links + "/" + this.target;
        targetGraph = Set.copyOf(graph(targetCells));
    }
    public List<Node> nodes() { return nodes; }
    public List<Link> links() { return links; }
    public List<Integer> target() { return target; }
    public int[] initial() { return initial.clone(); }
    public int[] targetCells() { return targetCells.clone(); }
    public boolean valid(int[] positions) { return validCells(positions, nodes.size()); }
    private static boolean validCells(int[] cells, int count) {
        if (cells.length != count) return false;
        int used = 0;
        for (int cell : cells) {
            if (cell < 0 || cell >= 25 || (used & (1 << cell)) != 0) return false;
            used |= 1 << cell;
        }
        return true;
    }
    // Compare the visible graph, not node IDs: equally coloured nodes are interchangeable.
    public boolean complete(int[] positions) { return valid(positions) && graph(positions).equals(targetGraph); }
    private Set<String> graph(int[] positions) {
        Set<String> graph = new HashSet<>();
        for (int i = 0; i < positions.length; i++) graph.add("node:" + endpoint(i, positions));
        for (var link : links) {
            String a = endpoint(link.from, positions), b = endpoint(link.to, positions);
            graph.add(a.compareTo(b) < 0 ? a + "/" + b : b + "/" + a);
        }
        return graph;
    }
    private String endpoint(int node, int[] positions) { return positions[node] + ":" + nodes.get(node).element.getSerializedName(); }
    public String signature() { return signature; }
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeCollection(nodes, (b, n) -> { b.writeEnum(n.element); b.writeVarInt(n.cell); });
        buffer.writeCollection(links, (b, l) -> { b.writeVarInt(l.from); b.writeVarInt(l.to); });
        buffer.writeCollection(target, (b, c) -> b.writeVarInt(c));
    }
    public static ExperimentDefinition read(RegistryFriendlyByteBuf buffer) {
        return new ExperimentDefinition(buffer.readList(b -> new Node(b.readEnum(Element.class), b.readVarInt())),
                buffer.readList(b -> new Link(b.readVarInt(), b.readVarInt())), buffer.readList(b -> b.readVarInt()));
    }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof ExperimentDefinition definition
                && nodes.equals(definition.nodes) && links.equals(definition.links) && target.equals(definition.target);
    }
    @Override public int hashCode() { return Objects.hash(nodes, links, target); }
    @Override public String toString() { return "ExperimentDefinition[nodes=" + nodes + ", links=" + links + ", target=" + target + "]"; }
}

package org.lyy.lyycore.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import java.util.*;

/** A small coloured graph whose vertices can move between the 25 cells. */
public record ExperimentDefinition(List<Node> nodes, List<Link> links, List<Integer> target) {
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

    public ExperimentDefinition {
        nodes = List.copyOf(nodes); links = List.copyOf(links); target = List.copyOf(target);
        if (nodes.isEmpty() || nodes.size() > 25 || links.isEmpty() || links.size() > 300
                || !validCells(nodes.stream().mapToInt(Node::cell).toArray(), nodes.size())
                || !validCells(target.stream().mapToInt(Integer::intValue).toArray(), nodes.size()))
            throw new IllegalArgumentException("Invalid experiment graph");
        for (var link : links) if (link.from < 0 || link.to < 0 || link.from >= nodes.size() || link.to >= nodes.size() || link.from == link.to)
            throw new IllegalArgumentException("Invalid experiment link");
    }
    public int[] initial() { return nodes.stream().mapToInt(Node::cell).toArray(); }
    public int[] targetCells() { return target.stream().mapToInt(Integer::intValue).toArray(); }
    public boolean valid(int[] positions) { return validCells(positions, nodes.size()); }
    private static boolean validCells(int[] cells, int count) {
        return cells.length == count && Arrays.stream(cells).allMatch(c -> c >= 0 && c < 25)
                && Arrays.stream(cells).distinct().count() == count;
    }
    // Compare the visible graph, not node IDs: equally coloured nodes are interchangeable.
    public boolean complete(int[] positions) { return valid(positions) && graph(positions).equals(graph(targetCells())); }
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
    public String signature() { return nodes + "/" + links + "/" + target; }
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeCollection(nodes, (b, n) -> { b.writeEnum(n.element); b.writeVarInt(n.cell); });
        buffer.writeCollection(links, (b, l) -> { b.writeVarInt(l.from); b.writeVarInt(l.to); });
        buffer.writeCollection(target, (b, c) -> b.writeVarInt(c));
    }
    public static ExperimentDefinition read(RegistryFriendlyByteBuf buffer) {
        return new ExperimentDefinition(buffer.readList(b -> new Node(b.readEnum(Element.class), b.readVarInt())),
                buffer.readList(b -> new Link(b.readVarInt(), b.readVarInt())), buffer.readList(b -> b.readVarInt()));
    }
}

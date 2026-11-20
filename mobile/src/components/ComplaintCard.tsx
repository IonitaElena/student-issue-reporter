import { Pressable, Text, View } from "react-native";
import { Complaint, CATEGORY_LABEL, PRIORITY_COLOR, PRIORITY_LABEL, STATUS_LABEL } from "../types";

type Props = {
  complaint: Complaint;
  canVote: boolean;
  onVote: () => void;
};

export default function ComplaintCard({ complaint: c, canVote, onVote }: Props) {
  return (
    <View style={{ backgroundColor: "white", borderRadius: 10, padding: 14, marginBottom: 12,
                   borderWidth: 1, borderColor: "#e5e7eb" }}>
      <View style={{ flexDirection: "row", justifyContent: "space-between", marginBottom: 6 }}>
        <Text style={{ color: "#6b7280", fontSize: 12 }}>
          {CATEGORY_LABEL[c.category] ?? c.category} · {new Date(c.createdAt).toLocaleDateString("ro-RO")}
        </Text>
        <Text style={{ color: PRIORITY_COLOR[c.priority], fontSize: 12, fontWeight: "bold" }}>
          {PRIORITY_LABEL[c.priority] ?? c.priority}
        </Text>
      </View>

      <Text style={{ fontSize: 16, fontWeight: "bold", marginBottom: 4 }}>{c.title}</Text>
      <Text numberOfLines={3} style={{ color: "#374151", marginBottom: 10 }}>{c.description}</Text>

      {(c.dormitory || c.building || c.locationText) && (
        <Text style={{ color: "#6b7280", fontSize: 12, marginBottom: 8 }}>
          📍 {[c.dormitory, c.building, c.locationText].filter(Boolean).join(" · ")}
        </Text>
      )}

      <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
        <Text style={{ fontSize: 12, backgroundColor: "#f3f4f6", paddingHorizontal: 8,
                       paddingVertical: 3, borderRadius: 10 }}>
          {STATUS_LABEL[c.status] ?? c.status}
        </Text>

        <Pressable onPress={onVote} disabled={!canVote}
          style={{ flexDirection: "row", alignItems: "center", paddingHorizontal: 12, paddingVertical: 6,
                   borderRadius: 16, backgroundColor: c.votedByMe ? "#2563eb" : "#f3f4f6",
                   opacity: canVote ? 1 : 0.5 }}>
          <Text style={{ color: c.votedByMe ? "white" : "#111827", fontWeight: "600" }}>
            👍 {c.voteCount}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}
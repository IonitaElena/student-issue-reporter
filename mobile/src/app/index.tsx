import { useCallback, useEffect, useRef, useState } from "react";
import { ActivityIndicator, Alert, Button, FlatList, Pressable, RefreshControl, ScrollView, Text, View } from "react-native";
import { Stack } from "expo-router";
import { api, errorMessage } from "../api";
import { useAuth } from "../auth/AuthContext";
import ComplaintCard from "../components/ComplaintCard";
import { CATEGORIES, CATEGORY_LABEL, Complaint, Page, VoteSummary } from "../types";

const PAGE_SIZE = 10;

export default function Home() {
  const { user, logout } = useAuth();
  const [items, setItems] = useState<Complaint[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [category, setCategory] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const busy = useRef(false); // evită cereri suprapuse

  const load = useCallback(async (pageToLoad: number, replace: boolean) => {
    if (busy.current) return;
    busy.current = true;
    setLoading(true);
    setError("");
    try {
      const res = await api.get<Page<Complaint>>("/api/complaints", {
        params: { page: pageToLoad, size: PAGE_SIZE, category: category ?? undefined },
      });
      setItems((prev) => (replace ? res.data.content : [...prev, ...res.data.content]));
      setPage(res.data.page);
      setTotalPages(res.data.totalPages);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      busy.current = false;
      setLoading(false);
      setRefreshing(false);
    }
  }, [category]);

  // prima încărcare și la schimbarea filtrului
  useEffect(() => {
    load(0, true);
  }, [load]);

  const onRefresh = () => {
    setRefreshing(true);
    load(0, true);
  };

  const onEndReached = () => {
    if (!loading && page + 1 < totalPages) load(page + 1, false);
  };

  const toggleVote = async (c: Complaint) => {
    try {
      const res = c.votedByMe
        ? await api.delete<VoteSummary>(`/api/complaints/${c.id}/votes`)
        : await api.post<VoteSummary>(`/api/complaints/${c.id}/votes`);
      setItems((prev) =>
        prev.map((x) => (x.id === c.id ? { ...x, voteCount: res.data.voteCount, votedByMe: res.data.votedByMe } : x))
      );
    } catch (e) {
      Alert.alert("Vot", errorMessage(e));
    }
  };

  return (
    <View style={{ flex: 1, backgroundColor: "#f9fafb" }}>
      <Stack.Screen options={{ headerRight: () => <Button title="Ieși" onPress={logout} /> }} />

      <View style={{ height: 52 }}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}
          contentContainerStyle={{ paddingHorizontal: 12, alignItems: "center" }}>
          {[null, ...CATEGORIES].map((cat) => (
            <Pressable key={cat ?? "all"} onPress={() => setCategory(cat)}
              style={{ paddingHorizontal: 14, paddingVertical: 8, borderRadius: 18, marginRight: 8,
                       backgroundColor: category === cat ? "#2563eb" : "#e5e7eb" }}>
              <Text style={{ color: category === cat ? "white" : "#111827" }}>
                {cat ? CATEGORY_LABEL[cat] : "Toate"}
              </Text>
            </Pressable>
          ))}
        </ScrollView>
      </View>

      {error ? <Text style={{ color: "crimson", padding: 12 }}>{error}</Text> : null}

      <FlatList
        data={items}
        keyExtractor={(c) => String(c.id)}
        contentContainerStyle={{ padding: 12 }}
        renderItem={({ item }) => (
          <ComplaintCard complaint={item} canVote={item.userId !== user?.userId}
            onVote={() => toggleVote(item)} />
        )}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        onEndReached={onEndReached}
        onEndReachedThreshold={0.4}
        ListFooterComponent={loading && !refreshing ? <ActivityIndicator style={{ margin: 16 }} /> : undefined}
        ListEmptyComponent={!loading ? (
          <Text style={{ textAlign: "center", color: "#6b7280", marginTop: 40 }}>
            Nu există sesizări.
          </Text>
        ) : undefined}
      />
    </View>
  );
}
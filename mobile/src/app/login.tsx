import { useState } from "react";
import { ActivityIndicator, Button, Text, TextInput, View } from "react-native";
import { Link } from "expo-router";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api";

export default function Login() {
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setError("");
    setBusy(true);
    try {
      await login(email, password);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={{ flex: 1, padding: 24, justifyContent: "center" }}>
      <Text style={{ fontSize: 28, fontWeight: "bold", marginBottom: 24 }}>CampusVoice</Text>
      <TextInput placeholder="Email instituțional" autoCapitalize="none" keyboardType="email-address"
        value={email} onChangeText={setEmail}
        style={{ borderWidth: 1, borderRadius: 6, padding: 12, marginBottom: 10 }} />
      <TextInput placeholder="Parolă" secureTextEntry value={password} onChangeText={setPassword}
        style={{ borderWidth: 1, borderRadius: 6, padding: 12, marginBottom: 10 }} />
      {error ? <Text style={{ color: "crimson", marginBottom: 10 }}>{error}</Text> : null}
      {busy ? <ActivityIndicator /> : <Button title="Intră în cont" onPress={submit} />}
      <Link href="/register" style={{ marginTop: 20, textAlign: "center", color: "royalblue" }}>
        Nu ai cont? Înregistrează-te
      </Link>
    </View>
  );
}
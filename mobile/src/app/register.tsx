import { useState } from "react";
import { ActivityIndicator, Button, ScrollView, Text, TextInput } from "react-native";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api";

export default function Register() {
  const { register } = useAuth();
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setError("");
    setBusy(true);
    try {
      await register(email, password, firstName, lastName);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const input = { borderWidth: 1, borderRadius: 6, padding: 12, marginBottom: 10 } as const;

  return (
    <ScrollView contentContainerStyle={{ padding: 24 }} keyboardShouldPersistTaps="handled">
      <TextInput placeholder="Prenume" value={firstName} onChangeText={setFirstName} style={input} />
      <TextInput placeholder="Nume" value={lastName} onChangeText={setLastName} style={input} />
      <TextInput placeholder="Email (nume@stud.facultate.upb.ro)" autoCapitalize="none"
        keyboardType="email-address" value={email} onChangeText={setEmail} style={input} />
      <TextInput placeholder="Parolă (minim 8 caractere)" secureTextEntry value={password}
        onChangeText={setPassword} style={input} />
      {error ? <Text style={{ color: "crimson", marginBottom: 10 }}>{error}</Text> : null}
      {busy ? <ActivityIndicator /> : <Button title="Creează cont" onPress={submit} />}
    </ScrollView>
  );
}
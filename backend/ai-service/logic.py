import math
import re
import unicodedata
import joblib
from sklearn.metrics.pairwise import cosine_similarity

LEVELS = ["LOW", "MEDIUM", "HIGH", "URGENT"]

URGENT_WORDS = ["scurtcircuit", "scurt circuit", "incendiu", "foc ", "fum", "miros de gaz",
                "scurgere de gaz", "gaz", "inundat", "electrocut", "pericol", "urgent",
                "ranit", "accident", "amenint", "agresat", "agresiune", "lift blocat",
                "blocat in lift", "ambulanta", "fara apa calda de o saptamana"]
HIGH_WORDS = ["nu functioneaza", "defect", "stricat", "fara apa", "fara caldura", "fara internet",
              "nu merge", "mucegai", "gandaci", "sobolani", "furt", "nu avem", "frig",
              "infiltratii", "curge", "termen", "examen", "restanta", "bursa"]


def normalize(t: str) -> str:
    t = unicodedata.normalize("NFKD", t.lower())
    return "".join(c for c in t if not unicodedata.combining(c))


class Models:
    def __init__(self, cat_path="category_model.joblib", pri_path="priority_model.joblib"):
        self.cat = joblib.load(cat_path)
        self.pri = joblib.load(pri_path)

    def embed(self, docs):
        return self.cat.named_steps["tfidf"].transform(docs)

    def classify(self, text: str):
        proba = self.cat.predict_proba([text])[0]
        classes = list(self.cat.classes_)
        ranked = sorted(zip(classes, proba), key=lambda x: -x[1])
        top = [{"category": c, "confidence": round(float(p), 3)} for c, p in ranked[:3]]
        return {"category": top[0]["category"], "confidence": top[0]["confidence"],
                "lowConfidence": top[0]["confidence"] < 0.45, "top3": top}

    def priority(self, text: str, votes: int = 0):
        model_level = str(self.pri.predict([text])[0])
        reasons = [f"model: {model_level}"]
        norm = normalize(text)
        idx = LEVELS.index(model_level)

        rule_level = None
        if any(w in norm for w in URGENT_WORDS):
            rule_level = "URGENT"
            reasons.append("cuvinte-cheie urgente")
        elif any(w in norm for w in HIGH_WORDS):
            rule_level = "HIGH"
            reasons.append("cuvinte-cheie de gravitate")
        if rule_level:
            idx = max(idx, LEVELS.index(rule_level))
        if votes >= 5 and idx < 2:  # voturile ridica pana la HIGH, nu pana la URGENT
            idx += 1
            reasons.append(f"{votes} voturi")
        return {"priority": LEVELS[idx], "reasons": reasons}


def haversine_m(lat1, lon1, lat2, lon2):
    r = 6371000
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def find_duplicates(text, lat, lon, candidates, embed, min_sim=0.3, max_dist_m=300):
    if not candidates:
        return []
    docs = [text] + [c["text"] for c in candidates]
    m = embed(docs)  # TF-IDF deja invatat pe setul de antrenare (idf stabil)
    sims = cosine_similarity(m[0], m[1:])[0]
    out = []
    for c, s in zip(candidates, sims):
        dist = None
        if lat is not None and lon is not None and c.get("latitude") is not None \
                and c.get("longitude") is not None:
            dist = haversine_m(lat, lon, c["latitude"], c["longitude"])
            if dist > max_dist_m:
                continue
        if s >= min_sim:
            out.append({"id": c["id"], "similarity": round(float(s), 3),
                        "distanceMeters": None if dist is None else round(dist)})
    return sorted(out, key=lambda x: -x["similarity"])
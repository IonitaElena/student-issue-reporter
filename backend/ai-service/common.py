import csv
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.pipeline import FeatureUnion, Pipeline
from sklearn.linear_model import LogisticRegression


def load_csv(path):
    texts, cats, pris = [], [], []
    with open(path, encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            texts.append(row["text"].strip())
            cats.append(row["category"].strip())
            pris.append(row["priority"].strip())
    return texts, cats, pris


def build_pipeline():
    # caractere (robust la greseli/lipsa diacriticelor) + cuvinte
    features = FeatureUnion([
        ("char", TfidfVectorizer(analyzer="char_wb", ngram_range=(2, 5),
                                 lowercase=True, strip_accents="unicode",
                                 sublinear_tf=True)),
        ("word", TfidfVectorizer(analyzer="word", ngram_range=(1, 2),
                                 lowercase=True, strip_accents="unicode",
                                 sublinear_tf=True)),
    ])
    return Pipeline([
        ("tfidf", features),
        ("clf", LogisticRegression(max_iter=2000, C=10, class_weight="balanced")),
    ])
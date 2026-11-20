"""Antrenare + evaluare. Rulare: python train.py [fisier_test.csv]"""
import sys
import joblib
import numpy as np
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from sklearn.metrics import classification_report, confusion_matrix
from common import load_csv, build_pipeline

TRAIN = "data/complaints_ro.csv"


def evaluate(name, y_true, y_pred):
    labels = sorted(set(y_true))
    print(f"\n=== {name} ===")
    print(classification_report(y_true, y_pred, labels=labels, zero_division=0))
    print("Matrice de confuzie (randuri=real, coloane=prezis):")
    print("labels:", labels)
    print(confusion_matrix(y_true, y_pred, labels=labels))


def main():
    X, y_cat, y_pri = load_csv(TRAIN)
    print(f"{len(X)} exemple")

    # 1) cross-validation (atentie: pe date sintetice scorul e optimist)
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    for name, y in (("CATEGORIE", y_cat), ("PRIORITATE", y_pri)):
        pred = cross_val_predict(build_pipeline(), X, y, cv=cv)
        print(f"\n{name}: acuratete CV = {np.mean(np.array(pred) == np.array(y)):.3f}")
        evaluate(f"{name} (5-fold CV)", y, list(pred))

    # 2) antrenare finala pe tot setul
    cat_model = build_pipeline().fit(X, y_cat)
    pri_model = build_pipeline().fit(X, y_pri)
    joblib.dump(cat_model, "category_model.joblib")
    joblib.dump(pri_model, "priority_model.joblib")
    print("\nModele salvate.")

    # 3) test scris de oameni (optional)
    if len(sys.argv) > 1:
        Xt, tc, tp = load_csv(sys.argv[1])
        evaluate("TEST uman - categorie", tc, list(cat_model.predict(Xt)))
        evaluate("TEST uman - prioritate", tp, list(pri_model.predict(Xt)))


if __name__ == "__main__":
    main()
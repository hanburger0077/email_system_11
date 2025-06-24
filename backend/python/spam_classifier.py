# python/spam_classifier.py
import sys
import joblib
import json
import os
from sklearn.feature_extraction.text import TfidfVectorizer

def predict(text):
    # 使用相对路径加载模型和向量化器
    script_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(script_dir, 'spam_model.pkl')
    vectorizer_path = os.path.join(script_dir, 'vectorizer.pkl')

    model = joblib.load(model_path)
    vectorizer = joblib.load(vectorizer_path)

    X = vectorizer.transform([text])
    prediction = model.predict(X)[0]
    prob = model.predict_proba(X).max()
    return {
        "is_spam": bool(prediction == 1),
        "confidence": float(prob)
    }

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"error": "缺少输入文本"}))
        sys.exit(1)

    text = sys.argv[1]
    result = predict(text)
    print(json.dumps(result))
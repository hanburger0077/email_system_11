# train_spam_model.py

import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline
from sklearn.metrics import accuracy_score
import joblib

# 1. 加载数据集
df = pd.read_csv('sms.tsv', sep='\t', header=None, names=['label', 'message'])

# 2. 标签编码：spam -> 1, ham -> 0
df['label'] = df['label'].map({'ham': 0, 'spam': 1})

# 3. 划分训练集和测试集
X_train, X_test, y_train, y_test = train_test_split(df['message'], df['label'], test_size=0.2, random_state=42)

# 4. 构建模型管道
pipeline = Pipeline([
    ('tfidf', TfidfVectorizer()),
    ('clf', MultinomialNB())
])

# 5. 训练模型
pipeline.fit(X_train, y_train)

# 6. 模型评估
y_pred = pipeline.predict(X_test)
print(f"模型准确率: {accuracy_score(y_test, y_pred):.2%}")

# 7. 保存模型和向量化器
joblib.dump(pipeline.named_steps['clf'], 'spam_model.pkl')
joblib.dump(pipeline.named_steps['tfidf'], 'vectorizer.pkl')

print("✅ 模型和向量化器已成功保存为 spam_model.pkl 和 vectorizer.pkl")
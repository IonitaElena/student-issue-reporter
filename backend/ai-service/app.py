from typing import List, Optional
from fastapi import FastAPI
from pydantic import BaseModel
from logic import Models, find_duplicates

app = FastAPI(title="CampusVoice AI Service")
models = Models()


class TextIn(BaseModel):
    text: str


class PriorityIn(BaseModel):
    text: str
    votes: int = 0


class Candidate(BaseModel):
    id: int
    text: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class DuplicatesIn(BaseModel):
    text: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    candidates: List[Candidate]


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/classify")
def classify(body: TextIn):
    return models.classify(body.text)


@app.post("/priority")
def priority(body: PriorityIn):
    return models.priority(body.text, body.votes)


@app.post("/duplicates")
def duplicates(body: DuplicatesIn):
    return {"duplicates": find_duplicates(
        body.text, body.latitude, body.longitude,
        [c.model_dump() for c in body.candidates], models.embed)}
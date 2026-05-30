
from nudenet import NudeDetector
from fastapi import FastAPI, File, UploadFile
import uvicorn
import tempfile
import os
import traceback

detector = NudeDetector()
app = FastAPI()

@app.post("/v1/detect")
async def detect(file: UploadFile = File(...)):
    try:
        contents = await file.read()
        with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as tmp:
            tmp.write(contents)
            tmp_path = tmp.name

        try:
            preds = detector.detect(tmp_path)
        except Exception as e:
            traceback.print_exc()
            return {"success": False, "prediction": [[]], "error": str(e)}
        finally:
            try:
                os.remove(tmp_path)
            except:
                pass

        return {
            "success": True,
            "prediction": [[
                {
                    "class": p["class"],
                    "score": float(p["score"]),
                    "box": [int(a) for a in p["box"]]
                }
                for p in (preds or [])
            ]]
        }

    except Exception as e:
        traceback.print_exc()
        return {"success": False, "prediction": [[]], "error": str(e)}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)


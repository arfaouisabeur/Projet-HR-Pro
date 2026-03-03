from diffusers import StableDiffusionImg2ImgPipeline
from PIL import Image
from flask import Flask, request, jsonify
import torch
import base64
from io import BytesIO

app = Flask(__name__)

print("Chargement du modèle...")

# Modèle anime léger, public, sans authentification
pipe = StableDiffusionImg2ImgPipeline.from_pretrained(
    "nitrosocke/Ghibli-Diffusion",
    torch_dtype=torch.float32
)
pipe = pipe.to("cpu")
pipe.safety_checker = None  # désactiver pour accélérer

print("Modèle chargé ! Serveur prêt.")

@app.route("/generate_avatar", methods=["POST"])
def generate_avatar():
    file = request.files["image"]
    init_image = Image.open(file).convert("RGB")
    init_image = init_image.resize((512, 512))  # requis par SD

    avatar = pipe(
        prompt="ghibli style anime portrait, high quality, studio ghibli",
        negative_prompt="blurry, bad quality, deformed",
        image=init_image,
        strength=0.65,
        guidance_scale=7.5,
        num_inference_steps=20   # réduit pour aller plus vite sur CPU
    ).images[0]

    buffer = BytesIO()
    avatar.save(buffer, format="PNG")
    img_str = base64.b64encode(buffer.getvalue()).decode("utf-8")

    return jsonify({"avatar": img_str})

app.run(port=5000)
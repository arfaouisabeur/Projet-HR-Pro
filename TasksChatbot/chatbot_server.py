import socket
import threading
import json
import re
import random
import time

class TaskAssistantAI:
    def __init__(self):
        print("🤖 Initializing AI Assistant (rule‑based mode)...")
        self.init_intents()
        print("✅ AI Assistant ready!")

    def init_intents(self):
        self.intents = {
            'greeting': {
                'patterns': ['bonjour', 'salut', 'hello', 'hey', 'coucou', 'bonsoir'],
                'responses': [
                    "Bonjour ! Comment puis-je vous aider ?",
                    "Salut ! Besoin d'aide avec vos tâches ?",
                    "Bonjour ! Je suis là pour vous assister."
                ]
            },
            'count_tasks': {
                'patterns': ['combien', 'nombre', 'compter', 'how many', 'count'],
                'responses': [
                    "Vous avez {count} tâches au total.",
                    "Actuellement, vous avez {count} tâches assignées."
                ]
            },
            'show_tasks': {
                'patterns': ['montre', 'affiche', 'liste', 'show', 'list', 'display'],
                'responses': [
                    "Voici vos tâches :\n{tasks}",
                    "J'ai trouvé ces tâches :\n{tasks}"
                ]
            },
            'filter_status': {
                'patterns': {
                    'todo': ['à faire', 'todo', 'a faire'],
                    'doing': ['en cours', 'doing', 'encours'],
                    'done': ['terminé', 'fini', 'done', 'complete']
                },
                'responses': [
                    "Tâches {status} :\n{tasks}",
                    "Voici les tâches {status} :\n{tasks}"
                ]
            },
            'search': {
                'patterns': ['cherche', 'recherche', 'trouve', 'search', 'find'],
                'responses': [
                    "J'ai trouvé ces tâches contenant '{query}':\n{tasks}",
                    "Résultats pour '{query}':\n{tasks}"
                ]
            },
            'advice': {
                'patterns': ['conseil', 'avis', 'suggestion', 'advice', 'help'],
                'responses': [
                    "💡 Conseil : {advice}",
                    "Voici un conseil : {advice}"
                ]
            },
            'productivity': {
                'patterns': ['productivité', 'efficacité', 'productivity', 'efficient'],
                'responses': [
                    "📊 Analyse de productivité :\n{analysis}"
                ]
            },
            'deadline': {
                'patterns': ['deadline', 'échéance', 'date limite', 'urgent'],
                'responses': [
                    "Tâches urgentes :\n{tasks}"
                ]
            }
        }

    def understand_intent(self, message):
        """Rule‑based intent detection (fast, works for French/English)."""
        msg = message.lower().strip()
        result = {
            'intent': 'unknown',
            'entities': {'status': None, 'numbers': [], 'dates': []},
            'original': message
        }

        # Check each intent
        for intent, config in self.intents.items():
            if intent == 'filter_status':
                for status, patterns in config['patterns'].items():
                    if any(p in msg for p in patterns):
                        result['intent'] = intent
                        result['entities']['status'] = status
                        return result
            else:
                for pattern in config['patterns']:
                    if pattern in msg:
                        result['intent'] = intent
                        return result

        # Try to extract numbers (maybe user asked for a task by ID)
        numbers = re.findall(r'\b\d+\b', msg)
        if numbers:
            result['entities']['numbers'] = [int(n) for n in numbers]
        return result

    def generate_advice(self, tasks):
        if not tasks:
            return "Vous n'avez pas de tâches pour le moment. C'est le moment d'en créer !"
        todo = sum(1 for t in tasks if t.get('statut') == 'TODO')
        doing = sum(1 for t in tasks if t.get('statut') == 'DOING')
        done = sum(1 for t in tasks if t.get('statut') == 'DONE')
        if todo > 5:
            return "Vous avez beaucoup de tâches en attente. Essayez d'en prioriser 3 maximum."
        if doing > 3:
            return "Vous avez trop de tâches en cours. Essayez d'en terminer quelques-unes."
        if done > todo + doing:
            return "Bon travail ! Vous terminez plus de tâches que vous n'en commencez."
        return "Maintenez ce rythme ! N'oubliez pas de faire des pauses."

    def analyze_productivity(self, tasks):
        if not tasks:
            return "Pas assez de données pour analyser votre productivité."
        total = len(tasks)
        done = sum(1 for t in tasks if t.get('statut') == 'DONE')
        rate = (done / total) * 100 if total else 0
        analysis = f"📈 Taux de complétion: {rate:.1f}%\n"
        if rate > 70:
            analysis += "🌟 Excellente productivité !"
        elif rate > 40:
            analysis += "👍 Bonne progression, continuez !"
        else:
            analysis += "💪 Vous pouvez améliorer votre taux de complétion."
        return analysis

    def generate_response(self, analysis, tasks):
        intent = analysis['intent']
        entities = analysis['entities']

        if intent == 'unknown':
            return ("Je n'ai pas bien compris. Vous pouvez me demander :\n"
                    "• Combien de tâches j'ai ?\n"
                    "• Montre mes tâches\n"
                    "• Conseil pour être plus productif\n"
                    "• Cherche [mot-clé]")

        if intent == 'greeting':
            return random.choice(self.intents['greeting']['responses'])

        if intent == 'count_tasks':
            count = len(tasks)
            return random.choice(self.intents['count_tasks']['responses']).format(count=count)

        if intent == 'show_tasks':
            if not tasks:
                return "Vous n'avez aucune tâche pour le moment."
            task_list = "\n".join(
                f"• #{t.get('id')} {t.get('titre')} ({t.get('statut')})"
                for t in tasks[:10]
            )
            if len(tasks) > 10:
                task_list += f"\n... et {len(tasks)-10} autre(s) tâche(s)"
            return random.choice(self.intents['show_tasks']['responses']).format(tasks=task_list)

        if intent == 'filter_status':
            status = entities.get('status')
            if not status:
                return "Quel statut voulez-vous voir ? (à faire, en cours, terminé)"
            status_map = {'todo': 'TODO', 'doing': 'DOING', 'done': 'DONE'}
            filtered = [t for t in tasks if t.get('statut') == status_map.get(status)]
            if not filtered:
                return f"Aucune tâche {status} trouvée."
            task_list = "\n".join(f"• #{t.get('id')} {t.get('titre')}" for t in filtered[:10])
            status_fr = {'todo': 'à faire', 'doing': 'en cours', 'done': 'terminées'}.get(status, status)
            return f"Tâches {status_fr} :\n{task_list}"

        if intent == 'search':
            # Use the original message as the query (you could refine)
            query = analysis['original']
            filtered = [
                t for t in tasks
                if query.lower() in t.get('titre', '').lower()
                or query.lower() in t.get('description', '').lower()
            ]
            if not filtered:
                return f"Aucune tâche trouvée pour '{query}'."
            task_list = "\n".join(f"• #{t.get('id')} {t.get('titre')}" for t in filtered[:10])
            return random.choice(self.intents['search']['responses']).format(query=query, tasks=task_list)

        if intent == 'advice':
            advice = self.generate_advice(tasks)
            return random.choice(self.intents['advice']['responses']).format(advice=advice)

        if intent == 'productivity':
            analysis_text = self.analyze_productivity(tasks)
            return random.choice(self.intents['productivity']['responses']).format(analysis=analysis_text)

        if intent == 'deadline':
            # Simple: show tasks in TODO as "urgent" (customize as needed)
            urgent = [t for t in tasks if t.get('statut') == 'TODO']
            if not urgent:
                return "Aucune tâche urgente pour le moment."
            task_list = "\n".join(f"• #{t.get('id')} {t.get('titre')}" for t in urgent[:5])
            return random.choice(self.intents['deadline']['responses']).format(tasks=task_list)

        return "Désolé, je n'ai pas de réponse pour cela."

    def process_message(self, message, tasks_data):
        try:
            analysis = self.understand_intent(message)
            return self.generate_response(analysis, tasks_data)
        except Exception as e:
            print(f"❌ Error in process_message: {e}")
            return "Désolé, une erreur interne s'est produite."


class ChatbotServer:
    def __init__(self, host='localhost', port=5000):
        self.host = host
        self.port = port
        self.ai = TaskAssistantAI()
        self.tasks_cache = []

    def start(self):
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((self.host, self.port))
        server.listen(5)
        print(f"🤖 AI Chatbot Server running on {self.host}:{self.port}")

        while True:
            client, address = server.accept()
            print(f"📱 Java client connected from {address}")
            threading.Thread(target=self.handle_client, args=(client,), daemon=True).start()

    def handle_client(self, client):
        with client:
            while True:
                try:
                    data = client.recv(4096).decode('utf-8')
                    if not data:
                        break

                    start = time.time()
                    print(f"[{time.strftime('%H:%M:%S')}] Received ({len(data)} bytes)")

                    # Handle context updates (no reply)
                    if data.startswith('__CONTEXT__'):
                        json_str = data[len('__CONTEXT__'):]
                        try:
                            self.tasks_cache = json.loads(json_str)
                            print(f"✅ Tasks updated: {len(self.tasks_cache)} tasks")
                        except json.JSONDecodeError as e:
                            print(f"❌ Invalid context JSON: {e}")
                        # Do NOT send a response for context updates
                        continue

                    # Otherwise, treat as a user message
                    user_message = data
                    print(f"User message: {user_message[:100]}")

                    # Process with AI
                    response = self.ai.process_message(user_message, self.tasks_cache)

                    elapsed = time.time() - start
                    print(f"[{time.strftime('%H:%M:%S')}] Response ({elapsed:.2f}s): {response[:100]}")

                    # Send response back
                    client.send((response + "\n").encode('utf-8'))

                except Exception as e:
                    print(f"❌ Error in handle_client: {e}")
                    try:
                        client.send("Désolé, une erreur technique est survenue.".encode('utf-8'))
                    except:
                        pass
                    break

if __name__ == "__main__":
    server = ChatbotServer()
    server.start()
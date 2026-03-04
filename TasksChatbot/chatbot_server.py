# chatbot_server.py
import socket
import threading
import json
import re
from datetime import datetime
import spacy
from textblob import TextBlob
import random

class TaskAssistantAI:
    def __init__(self):
        print("🤖 Initializing AI Assistant...")
        # Load spaCy model for NLP
        try:
            self.nlp = spacy.load("en_core_web_sm")
        except:
            print("Downloading spaCy model...")
            import subprocess
            subprocess.run(["python", "-m", "spacy", "download", "en_core_web_sm"])
            self.nlp = spacy.load("en_core_web_sm")
        
        # Context memory
        self.context = {}
        self.conversation_history = []
        
        # Intent patterns with synonyms
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
                'patterns': ['à faire', 'en cours', 'terminé', 'todo', 'doing', 'done'],
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
        
        # French stop words
        self.stop_words = set(['le', 'la', 'les', 'un', 'une', 'des', 'et', 'ou', 'mais',
                               'donc', 'car', 'ni', 'mon', 'ton', 'son', 'mes', 'tes', 'ses'])
        
        print("✅ AI Assistant ready!")
    
    def understand_intent(self, message):
        """NLP-based intent understanding"""
        doc = self.nlp(message.lower())
        
        # Extract keywords (nouns and verbs)
        keywords = [token.lemma_ for token in doc 
                   if token.pos_ in ['NOUN', 'VERB'] 
                   and token.text not in self.stop_words]
        
        # Sentiment analysis
        blob = TextBlob(message)
        sentiment = blob.sentiment
        
        # Entity extraction
        entities = {
            'dates': [],
            'numbers': [],
            'status': None
        }
        
        # Find status
        status_keywords = {
            'todo': ['faire', 'todo', 'à faire'],
            'doing': ['cours', 'doing', 'encours', 'progress'],
            'done': ['terminé', 'fini', 'done', 'complete']
        }
        
        for status, keywords_list in status_keywords.items():
            if any(kw in message.lower() for kw in keywords_list):
                entities['status'] = status
                break
        
        # Find numbers (task IDs, counts)
        for token in doc:
            if token.like_num:
                entities['numbers'].append(int(token.text))
        
        # Find dates
        from dateparser import parse
        date_patterns = re.findall(r'\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}-\d{2}-\d{2}', message)
        if date_patterns:
            entities['dates'].extend(date_patterns)
        
        # Match intent using fuzzy matching
        best_intent = 'unknown'
        best_score = 0
        
        for intent, config in self.intents.items():
            for pattern in config['patterns']:
                # Calculate similarity score
                pattern_words = set(pattern.split())
                message_words = set(keywords)
                
                if pattern_words and message_words:
                    intersection = pattern_words.intersection(message_words)
                    score = len(intersection) / max(len(pattern_words), 1)
                    
                    if score > best_score and score > 0.3:
                        best_score = score
                        best_intent = intent
        
        # Check for direct status mentions
        if entities['status']:
            best_intent = 'filter_status'
        
        return {
            'intent': best_intent,
            'keywords': keywords,
            'entities': entities,
            'sentiment': sentiment,
            'confidence': best_score,
            'original': message
        }
    
    def generate_advice(self, tasks):
        """Generate smart advice based on tasks"""
        if not tasks:
            return "Vous n'avez pas de tâches pour le moment. C'est le moment d'en créer !"
        
        todo_count = sum(1 for t in tasks if t.get('statut') == 'TODO')
        doing_count = sum(1 for t in tasks if t.get('statut') == 'DOING')
        done_count = sum(1 for t in tasks if t.get('statut') == 'DONE')
        
        if todo_count > 5:
            return "Vous avez beaucoup de tâches en attente. Essayez d'en prioriser 3 maximum."
        elif doing_count > 3:
            return "Vous avez trop de tâches en cours. Essayez d'en terminer quelques-unes."
        elif done_count > todo_count + doing_count:
            return "Bon travail ! Vous terminez plus de tâches que vous n'en commencez."
        else:
            return "Maintenez ce rythme ! N'oubliez pas de faire des pauses."
    
    def analyze_productivity(self, tasks):
        """Analyze productivity patterns"""
        if not tasks:
            return "Pas assez de données pour analyser votre productivité."
        
        # Simple analysis
        total = len(tasks)
        done = sum(1 for t in tasks if t.get('statut') == 'DONE')
        completion_rate = (done / total) * 100 if total > 0 else 0
        
        analysis = f"📈 Taux de complétion: {completion_rate:.1f}%\n"
        
        if completion_rate > 70:
            analysis += "🌟 Excellente productivité !"
        elif completion_rate > 40:
            analysis += "👍 Bonne progression, continuez !"
        else:
            analysis += "💪 Vous pouvez améliorer votre taux de complétion."
        
        return analysis
    
    def generate_response(self, analysis, tasks, user_context):
        """Generate AI response based on intent"""
        intent = analysis['intent']
        entities = analysis['entities']
        
        if intent == 'greeting':
            return random.choice(self.intents['greeting']['responses'])
        
        elif intent == 'count_tasks':
            count = len(tasks)
            return random.choice(self.intents['count_tasks']['responses']).format(count=count)
        
        elif intent == 'show_tasks':
            if not tasks:
                return "Vous n'avez aucune tâche pour le moment."
            
            task_list = "\n".join([
                f"• #{t.get('id')} {t.get('titre')} ({t.get('statut')})"
                for t in tasks[:10]  # Show only first 10
            ])
            
            if len(tasks) > 10:
                task_list += f"\n... et {len(tasks) - 10} autre(s) tâche(s)"
            
            return random.choice(self.intents['show_tasks']['responses']).format(tasks=task_list)
        
        elif intent == 'filter_status':
            status = entities['status']
            if not status:
                return "Quel statut voulez-vous voir ? (à faire, en cours, terminé)"
            
            status_map = {'todo': 'TODO', 'doing': 'DOING', 'done': 'DONE'}
            filtered = [t for t in tasks if t.get('statut') == status_map.get(status, '')]
            
            if not filtered:
                return f"Aucune tâche {status} trouvée."
            
            task_list = "\n".join([
                f"• #{t.get('id')} {t.get('titre')}"
                for t in filtered[:10]
            ])
            
            status_fr = {'todo': 'à faire', 'doing': 'en cours', 'done': 'terminées'}[status]
            return f"Tâches {status_fr} :\n{task_list}"
        
        elif intent == 'advice':
            advice = self.generate_advice(tasks)
            return random.choice(self.intents['advice']['responses']).format(advice=advice)
        
        elif intent == 'productivity':
            analysis = self.analyze_productivity(tasks)
            return random.choice(self.intents['productivity']['responses']).format(analysis=analysis)
        
        elif intent == 'search':
            query = ' '.join(analysis['keywords'])
            if not query:
                return "Que voulez-vous chercher ?"
            
            filtered = [
                t for t in tasks 
                if query.lower() in t.get('titre', '').lower() 
                or query.lower() in t.get('description', '').lower()
            ]
            
            if not filtered:
                return f"Aucune tâche trouvée pour '{query}'."
            
            task_list = "\n".join([
                f"• #{t.get('id')} {t.get('titre')}"
                for t in filtered[:10]
            ])
            
            return random.choice(self.intents['search']['responses']).format(query=query, tasks=task_list)
        
        else:
            # Default response
            return "Je n'ai pas bien compris. Vous pouvez me demander :\n" + \
                   "• Combien de tâches j'ai ?\n" + \
                   "• Montre mes tâches\n" + \
                   "• Conseil pour être plus productif\n" + \
                   "• Cherche [mot-clé]"
    
    def process_message(self, message, tasks_data):
        """Main processing function"""
        # Understand intent
        analysis = self.understand_intent(message)
        
        # Generate response
        response = self.generate_response(analysis, tasks_data, {})
        
        # Add personality
        if analysis['sentiment'].polarity < -0.3:
            response = "😟 " + response + " Voulez-vous en parler ?"
        elif analysis['sentiment'].polarity > 0.3:
            response = "😊 " + response
        
        return response

# Socket server to communicate with Java
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
            threading.Thread(target=self.handle_client, args=(client,)).start()
    
    def handle_client(self, client):
        with client:
            while True:
                try:
                    # Receive message from Java
                    data = client.recv(4096).decode('utf-8')
                    if not data:
                        break
                    
                    print(f"📨 Received: {data}")
                    
                    # Parse JSON if present
                    try:
                        message_data = json.loads(data)
                        message = message_data.get('message', '')
                        self.tasks_cache = message_data.get('tasks', [])
                    except:
                        message = data
                    
                    # Process with AI
                    response = self.ai.process_message(message, self.tasks_cache)
                    
                    # Send response back
                    client.send(response.encode('utf-8'))
                    print(f"📤 Sent: {response}")
                    
                except Exception as e:
                    print(f"❌ Error: {e}")
                    break

if __name__ == "__main__":
    server = ChatbotServer()
    server.start()
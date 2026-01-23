#!/bin/bash
echo "🔨 Compilation de ZombieMod v1.0.1"
echo "=================================="
echo ""
echo "Étape 1/3 : Vérification de Java..."
java -version
echo ""
echo "Étape 2/3 : Nettoyage des anciens fichiers..."
./gradlew clean
echo ""
echo "Étape 3/3 : Compilation du mod..."
./gradlew build
echo ""
if [ -f "build/libs/zombiemod-1.0.1.jar" ]; then
    echo "✅ SUCCÈS ! Le mod a été compilé :"
    echo "📦 Fichier : build/libs/zombiemod-1.0.1.jar"
    ls -lh build/libs/zombiemod-1.0.1.jar
else
    echo "❌ ÉCHEC : Le fichier JAR n'a pas été créé"
fi

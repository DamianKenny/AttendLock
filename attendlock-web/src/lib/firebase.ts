import { initializeApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';
import { getAuth } from 'firebase/auth';

// TODO: Replace with your Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyBQ7ZfgEEdXkkZOVQupsfEjQRj-65ukZq8",
  authDomain: "attendlock-f3bf6.firebaseapp.com",
  projectId: "attendlock-f3bf6",
  storageBucket: "attendlock-f3bf6.firebasestorage.app",
  messagingSenderId: "446952572837",
  appId: "1:446952572837:web:06112767137a0a940dca4c",
  measurementId: "G-VLWNCPKDR1"
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const storage = getStorage(app);
export const auth = getAuth(app);

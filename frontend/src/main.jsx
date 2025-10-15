import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ThemeProvider as MTThemeProvider } from '@material-tailwind/react'
import { ThemeProvider } from './contexts/ThemeContext.jsx'
import './index.css'
import './App.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider>
      <MTThemeProvider>
        <App />
      </MTThemeProvider>
    </ThemeProvider>
  </StrictMode>,
)

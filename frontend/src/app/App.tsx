import { Container, Typography } from '@mui/material'

export default function App() {
  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" component="h1">
        DESS
      </Typography>
      <Typography color="text.secondary">
        Frontend skeleton. Build the screens once the API workflow is stable.
      </Typography>
    </Container>
  )
}

async function login () {
  try {
    const response = await axios.post('login', getLoginParameters())
    axios.defaults.headers.common['Authorization'] = 'Bearer ' + response.data
    switchViewTo('feed-view')
    notify('Login effettuato con successo')
    document.getElementById('navbar').classList.remove('hidden')
    document.getElementById(
      'my-username'
    ).innerHTML = getLoginParameters().split('\n')[0]
  } catch {
    showErrorNotification('Username o password errati.')
  }
}

async function logout () {
  try {
    await axios.post('logout')
    axios.defaults.headers.common['Authorization'] = undefined
    switchViewTo('login-view')
    notify('Logout effettuato con successo')
    document.getElementById('navbar').classList.add('hidden')
  } catch {
    showErrorNotification('Si è verificato un errore durante il logout.')
  }
}

async function createPost () {}

async function createComment () {}

async function upVotePost () {}

async function downVotePost () {}

function getLoginParameters () {
  return `${document.getElementById('form-username').value}\n${
    document.getElementById('form-password').value
  }`
}

function showErrorNotification (msg) {
  alert(msg)
}

function switchViewTo (viewId) {
  document
    .getElementsByClassName('current-view')[0]
    .classList.remove('current-view')
  document.getElementById(viewId).classList.add('current-view')
}

function notify (msg) {
  document.getElementById('notification').classList.remove('opacity-0')
  document.getElementById('notification').classList.add('opacity-100')
  document.getElementById('notification').innerHTML = msg
  setTimeout(() => {
    document.getElementById('notification').classList.remove('opacity-100')
    document.getElementById('notification').classList.add('opacity-0')
  }, 3000)
}

function showComments (postId) {
  document
    .getElementById('post-' + postId + '-comments')
    .classList.toggle('hidden')
  const toggle = document.getElementById('post-' + postId + '-comments-toggle')
  toggle.innerHTML =
    toggle.innerHTML[0] == 'M' ? 'Nascondi commenti' : 'Mostra commenti'
}

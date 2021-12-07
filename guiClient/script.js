async function login () {
  try {
    const response = await axios.post('login', getLoginParameter())
    axios.defaults.headers.common['Authorization'] = 'Bearer ' + response.data
    switchViewTo('feed')
  } catch {
    showErrorNotification('Username o password errati.')
  }
}

async function createPost () {}

async function createComment () {}

async function upVotePost () {}

async function downVotePost () {}

function getLoginParameter () {
  return `${document.getElementById('form-username').value}\n${
    document.getElementById('form-password').value
  }`
}

function showErrorNotification (msg) {
  alert(msg)
}

function switchViewTo (viewId) {}

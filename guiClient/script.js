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
    await getUsers()
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

async function getUsers() {
  try {
    const response = await axios.get("users")
    console.log(response.data)
  } catch {
    showErrorNotification("Si è verificato un errore accedendo alla lista degli utenti. Riprova.")
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

function getPostHtml (id, title, content, username, comments) {
  let ret = `<div id="post-${id}" class="w-full p-6 mx-auto my-4 border border-gray-300 rounded-md post hover:shadow-inner">
                  <div class="flex mb-4 space-x-2">
                  <div class="w-10 h-10 rounded-full shadow-md avatar bg-gray-50"></div>
                <div class="flex space-x-2">
                  <h3 class="my-auto text-xl font-medium text-blue-800">${title}</h3>
                  <p class="my-auto text-sm text-gray-400 font-light">&mdash; di ${username}</p>
                </div>
                </div>
                    <div class="flex">
                        <div class="w-11/12">
                            <p>${content}</p>
                        </div>
                        <div class="ml-auto flex flex-col space-y-6">
                            <button class="py-1 px-2 rounded-xl transition-colors duration-75 text-green-800 hover:text-green-900 hover:bg-green-200 active:bg-green-300 font-semibold" onclick="vote('postId', 1)">+1</button>
                            <button class="py-1 px-2 rounded-xl transition-colors duration-75 text-red-800 hover:text-red-900 hover:bg-red-200 active:bg-red-300 font-semibold" onclick="vote('postId', -1)">&minus;1</button>
                        </div>
                    </div>
                    <p id="post-${id}-comments-toggle" onclick="showComments('${id}')" class="mt-6 mb-4 text-blue-900 cursor-pointer hover:underline">Mostra commenti</p>
                    <div id="post-${id}-comments" class="hidden">
                        ${getCommentsHtml(comments)}
                        <div class="flex space-x-2">
                            <input id="post-${id}-comment-input" type="text" placeholder="Commenta..." class="rounded-full py-2 px-3 border border-gray-300 w-4/5" />
                            <button class="bg-blue-800 hover:bg-blue-900 rounded-xl shadow-md py-1 px-3 my-auto text-white" onclick="addComment('${id}')">Invia</button>
                        </div>
                    </div>
                </div>`
  return ret
}

function getCommentsHtml (comments) {
  return comments.reduce(
    (acc, comment) =>
      (acc += `<div class="my-0.5">
                            <div class="flex mb-6 space-x-2">
                                <div class="flex space-x-1">
                                    <div class="w-4 h-4 my-auto rounded-full shadow-md avatar bg-gray-50"></div>
                                <p class="text-blue-800 font-medium">${comment.username}</p>
                                </div>
                                <p class="my-auto">${comment.content}</p>
                            </div>
                        </div>`),
    ''
  )
}

function togglePostCreation () {
  document.getElementById('post-creation-backdrop').classList.toggle('hidden')
  document.getElementById('post-creation-modal').classList.toggle('hidden')
}

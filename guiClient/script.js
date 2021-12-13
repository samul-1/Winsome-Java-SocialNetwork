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
    return
  }
  await getUsers()
  await getMyPosts()
  await getFeed()
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

async function getUsers () {
  try {
    const response = await axios.get('users')
    document.getElementById('user-list-content').innerHTML = getUserListHtml(
      response.data
    )
    console.log(response.data)
  } catch {
    showErrorNotification(
      'Si è verificato un errore accedendo alla lista degli utenti. Riprova.'
    )
  }
}

async function getMyPosts () {
  const response = await axios.get('posts/my-posts')
  console.log(response.data)
  response.data.forEach(post => {
    console.log(post)
    document.getElementById('my-posts-content').innerHTML += getPostHtml(post)
  })
}

async function getFeed () {
  const response = await axios.get('posts')
  console.log(response.data)
  response.data.forEach(post => {
    console.log(post)
    document.getElementById('feed-content').innerHTML += getPostHtml(post)
  })
}

async function createPost () {
  try {
    const response = await axios.post('posts', {
      title: document.getElementById('new-post-title').value,
      content: document.getElementById('new-post-content').value
    })
    const myPostsContent = document.getElementById('my-posts-content')
    myPostsContent.innerHTML =
      getPostHtml(response.data) + myPostsContent.innerHTML
    notify('Post creato con successo')
    console.log(response)
    togglePostCreation()
  } catch {
    showErrorNotification(
      'Si è verificato un errore durante la creazione del post. Riprova.'
    )
  }
}

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
  // const btn = document.getElementById(viewId + '-btn')
  // if (btn) {
  //   btn.classList.toggle('opacity-60')
  //   btn.classList.toggle('cursor-not-allowed')
  //   btn.classList.toggle('pointer-events-none')
  // }
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

function getPostHtml (post) {
  let ret = `<div id="post-${
    post.id
  }" class="w-full p-6 mx-auto my-4 border border-gray-300 rounded-md post hover:shadow-inner">
                  <div class="flex mb-4 space-x-2">
                  <div class="w-10 h-10 rounded-full shadow-md avatar bg-gray-50"></div>
                <div class="flex space-x-2">
                  <h3 class="my-auto text-xl font-medium text-blue-800">${
                    post.title
                  }</h3>
                  <p class="my-auto text-sm text-gray-400 font-light">&mdash; di ${
                    post.author
                  }</p>
                </div>
                </div>
                    <div class="flex">
                        <div class="w-11/12">
                            <p>${post.content}</p>
                        </div>
                        <div class="ml-auto flex flex-col space-y-6">
                            <button class="py-1 px-2 rounded-xl transition-colors duration-75 text-green-800 hover:text-green-900 hover:bg-green-200 active:bg-green-300 font-semibold" onclick="vote('postId', 1)">+1</button>
                            <button class="py-1 px-2 rounded-xl transition-colors duration-75 text-red-800 hover:text-red-900 hover:bg-red-200 active:bg-red-300 font-semibold" onclick="vote('postId', -1)">&minus;1</button>
                        </div>
                    </div>
                    <p id="post-${
                      post.id
                    }-comments-toggle" onclick="showComments('${
    post.id
  }')" class="mt-6 mb-4 text-blue-900 cursor-pointer hover:underline">Mostra commenti</p>
                    <div id="post-${post.id}-comments" class="hidden">
                        ${getCommentsHtml(post.comments)}
                        <div class="flex space-x-2">
                            <input id="post-${
                              post.id
                            }-comment-input" type="text" placeholder="Commenta..." class="rounded-full py-2 px-3 border border-gray-300 w-4/5" />
                            <button class="bg-blue-800 hover:bg-blue-900 rounded-xl shadow-md py-1 px-3 my-auto text-white" onclick="addComment('${
                              post.id
                            }')">Invia</button>
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
                                    <p class="text-blue-800 font-medium">${comment.authorUsername}</p>
                                </div>
                                <p class="my-auto">${comment.content}</p>
                            </div>
                        </div>`),
    ''
  )
}

function getUserListHtml (userList) {
  return userList.reduce(
    (acc, user) =>
      (acc += `<div class="flex my-3">
                  <div class="w-4 h-4 my-auto rounded-full shadow-md avatar bg-gray-50"></div>
                  <p class="ml-1 mr-4 text-blue-800 font-medium">${
                    user.username
                  }</p>
                  <div class="flex space-x-2">
                    ${getTagsHtml(user.tags)}
                  </div>
                  <button class="ml-auto rounded-md px-4 py-0.5 text-sm bg-green-500 text-white hover:bg-green-600">Segui</button>
               </div>`),
    ''
  )
}

function getTagsHtml (tagList) {
  return tagList.reduce((acc, tag) => {
    const { bg, text } = getRandomTagColor()
    acc += `<div style="min-width: 75px" class="text-center px-2 py-0.5 rounded-full shadow-sm opacity-90 text-sm text-${text} bg-${bg}">${tag}</div>`
    return acc
  }, '')
}

function getRandomTagColor () {
  const colors = ['yellow', 'green', 'blue', 'indigo', 'purple', 'red']
  const color = colors[Math.round(Math.random() * (colors.length - 1))]
  return {
    bg: color + '-300',
    text: color + '-900'
  }
}

function togglePostCreation () {
  document.getElementById('post-creation-backdrop').classList.toggle('hidden')
  document.getElementById('post-creation-modal').classList.toggle('hidden')
}

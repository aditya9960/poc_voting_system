from flask import Blueprint, request, jsonify
from werkzeug.security import generate_password_hash, check_password_hash
from sqlalchemy.exc import IntegrityError
from . import db
from datetime import datetime
from .models import User, FeatureRequest, Vote

bp = Blueprint('api', __name__, url_prefix='/api')


@bp.route('/')
def index():
    return "Welcome to the Feature Voting System API!"

@bp.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'healthy', 'timestamp': datetime.utcnow().isoformat()})


@bp.route('/api/users', methods=['POST'])
def create_user():
    data = request.get_json()

    if not data or not all(k in data for k in ('username', 'email', 'password')):
        return jsonify({'error': 'Missing required fields'}), 400

    try:
        password_hash = generate_password_hash(data['password'])
        user = User(
            username=data['username'],
            email=data['email'],
            password_hash=password_hash
        )
        db.session.add(user)
        db.session.commit()

        return jsonify({
            'id': user.id,
            'username': user.username,
            'email': user.email,
            'created_at': user.created_at.isoformat()
        }), 201
    except IntegrityError:
        db.session.rollback()
        return jsonify({'error': 'Username or email already exists'}), 409


@bp.route('/api/features', methods=['GET'])
def get_features():
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 10, type=int)
    sort_by = request.args.get('sort_by', 'vote_count')  # vote_count or created_at

    query = FeatureRequest.query

    if sort_by == 'vote_count':
        query = query.order_by(FeatureRequest.vote_count.desc())
    else:
        query = query.order_by(FeatureRequest.created_at.desc())

    features = query.paginate(page=page, per_page=per_page, error_out=False)

    return jsonify({
        'features': [{
            'id': f.id,
            'title': f.title,
            'description': f.description,
            'author': f.author.username,
            'status': f.status,
            'vote_count': f.vote_count,
            'created_at': f.created_at.isoformat(),
            'updated_at': f.updated_at.isoformat()
        } for f in features.items],
        'pagination': {
            'page': features.page,
            'per_page': features.per_page,
            'total': features.total,
            'pages': features.pages,
            'has_next': features.has_next,
            'has_prev': features.has_prev
        }
    })


@bp.route('/api/features', methods=['POST'])
def create_feature():
    data = request.get_json()

    if not data or not all(k in data for k in ('title', 'description', 'user_id')):
        return jsonify({'error': 'Missing required fields'}), 400

    # Verify user exists
    user = User.query.get(data['user_id'])
    if not user:
        return jsonify({'error': 'User not found'}), 404

    feature = FeatureRequest(
        title=data['title'],
        description=data['description'],
        user_id=data['user_id']
    )

    db.session.add(feature)
    db.session.commit()

    return jsonify({
        'id': feature.id,
        'title': feature.title,
        'description': feature.description,
        'author': feature.author.username,
        'status': feature.status,
        'vote_count': feature.vote_count,
        'created_at': feature.created_at.isoformat()
    }), 201


@bp.route('/api/features/<int:feature_id>', methods=['GET'])
def get_feature(feature_id):
    feature = FeatureRequest.query.get_or_404(feature_id)

    return jsonify({
        'id': feature.id,
        'title': feature.title,
        'description': feature.description,
        'author': feature.author.username,
        'status': feature.status,
        'vote_count': feature.vote_count,
        'created_at': feature.created_at.isoformat(),
        'updated_at': feature.updated_at.isoformat()
    })


@bp.route('/api/features/<int:feature_id>/vote', methods=['POST'])
def vote_feature(feature_id):
    data = request.get_json()

    if not data or 'user_id' not in data:
        return jsonify({'error': 'Missing user_id'}), 400

    # Verify feature exists
    feature = FeatureRequest.query.get_or_404(feature_id)

    # Verify user exists
    user = User.query.get(data['user_id'])
    if not user:
        return jsonify({'error': 'User not found'}), 404

    # Check if user already voted
    existing_vote = Vote.query.filter_by(
        user_id=data['user_id'],
        feature_request_id=feature_id
    ).first()

    if existing_vote:
        return jsonify({'error': 'User already voted for this feature'}), 409

    # Create vote
    vote = Vote(user_id=data['user_id'], feature_request_id=feature_id)
    db.session.add(vote)

    # Update vote count (or rely on database trigger)
    feature.vote_count += 1

    db.session.commit()

    return jsonify({
        'message': 'Vote added successfully',
        'feature_id': feature_id,
        'new_vote_count': feature.vote_count
    }), 201


@bp.route('/api/features/<int:feature_id>/vote', methods=['DELETE'])
def remove_vote(feature_id):
    data = request.get_json()

    if not data or 'user_id' not in data:
        return jsonify({'error': 'Missing user_id'}), 400

    # Find the vote
    vote = Vote.query.filter_by(
        user_id=data['user_id'],
        feature_request_id=feature_id
    ).first()

    if not vote:
        return jsonify({'error': 'Vote not found'}), 404

    # Remove vote
    db.session.delete(vote)

    # Update vote count
    feature = FeatureRequest.query.get(feature_id)
    feature.vote_count -= 1

    db.session.commit()

    return jsonify({
        'message': 'Vote removed successfully',
        'feature_id': feature_id,
        'new_vote_count': feature.vote_count
    })


@bp.route('/api/users/<int:user_id>/votes', methods=['GET'])
def get_user_votes(user_id):
    """Get all feature IDs that the user has voted for"""
    votes = Vote.query.filter_by(user_id=user_id).all()
    voted_feature_ids = [vote.feature_request_id for vote in votes]

    return jsonify({
        'user_id': user_id,
        'voted_features': voted_feature_ids
    })


@bp.errorhandler(404)
def not_found(error):
    return jsonify({'error': 'Resource not found'}), 404


@bp.errorhandler(500)
def internal_error(error):
    db.session.rollback()
    return jsonify({'error': 'Internal server error'}), 500

# @bp.route('/features', methods=['GET'])
# def get_features():
#     features = Feature.query.order_by(Feature.votes.desc()).all()
#     return jsonify([{"id": f.id, "title": f.title, "description": f.description, "votes": f.votes} for f in features])
#
# @bp.route('/features', methods=['POST'])
# def post_feature():
#     data = request.get_json()
#     new_feature = Feature(title=data['title'], description=data.get('description', ''))
#     db.session.add(new_feature)
#     db.session.commit()
#     return jsonify({"message": "Feature created", "id": new_feature.id}), 201
#
# @bp.route('/features/<int:id>/upvote', methods=['POST'])
# def upvote(id):
#     feature = Feature.query.get_or_404(id)
#     feature.votes += 1
#     db.session.commit()
#     return jsonify({"message": "Upvoted", "votes": feature.votes})
